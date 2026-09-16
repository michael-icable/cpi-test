// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4.
/*
 * MeasTaskSync.groovy
 *
 * Script steps of the iFlow "Frigibin MeasurementTask to SAP Sync".
 * Every step function is thin; the logic lives in the pure, unit-tested class
 * SyncLogic at the end of this file (tests: cpi/test).
 *
 * Exchange properties (prefix "sync."):
 *   sync.targetHost        S/4 virtual host = credential alias = Data Store entry ID
 *   sync.locationId        Cloud Connector location ID of the target
 *   sync.runId             unique ID of the run
 *   sync.lastVersion       watermark read from the Data Store (Long, -1 = none)
 *   sync.newVersion        CT version returned by the delta procedure (Long)
 *   sync.isFullLoad        "true" | "false"
 *   sync.step              current step (error context)
 */
import com.sap.it.script.v2.api.Message
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper
import java.time.Instant

// ---------------------------------------------------------------------------
// Step functions
// ---------------------------------------------------------------------------

/** Validates the configuration of the run and prepares the Data Store read. */
def Message initRun(Message message) {
    def host = SyncLogic.requireText(message.getProperty('sync.targetHost'), 'sync.targetHost')
    message.setProperty('sync.targetHost', host)
    message.setProperty('sync.interfaceId', message.getProperty('sync.interfaceId') ?: SyncLogic.INTERFACE_ID)
    message.setProperty('sync.runId', UUID.randomUUID().toString())
    message.setProperty('sync.startedAt', Instant.now().toString())
    message.setProperty('sync.packageSize', SyncLogic.toPositiveInt(message.getProperty('sync.packageSize'), 'sync.packageSize', 1, 5000))
    message.setProperty('sync.pageSize', SyncLogic.toPositiveInt(message.getProperty('sync.pageSize'), 'sync.pageSize', 1, 5000))
    message.setProperty('sync.warningCount', 0L)
    message.setProperty('sync.warningDetails', [])
    message.setProperty('sync.step', 'Read watermark')
    addCustomHeader(message, 'TargetHost', host)
    message.setBody('')
    return message
}

/** Reads the watermark (Data Store body) and builds the JDBC call of the delta procedure. */
def Message prepareDeltaRequest(Message message) {
    def lastVersion = SyncLogic.parseWatermark(message.getBody(String))
    message.setProperty('sync.lastVersion', lastVersion)
    message.setProperty('sync.step', 'Read delta')
    addCustomHeader(message, 'LastVersion', lastVersion.toString())
    message.setBody(SyncLogic.buildProcedureCall('dbo.usp_GetMeasurementTaskDelta',
            [[name: 'LastSyncVersion', type: 'BIGINT', value: lastVersion]]))
    return message
}

/** Evaluates the delta procedure result and prepares the packages for S/4. */
def Message evaluateDelta(Message message) {
    def json = SyncLogic.extractPayload(message.getBody(String))
    logPayload(message, 'Delta', json)
    def delta = SyncLogic.parseDelta(json)
    int packageSize = message.getProperty('sync.packageSize') as int

    message.setProperty('sync.newVersion', delta.syncVersion)
    message.setProperty('sync.isFullLoad', delta.isFullLoadRequired.toString())
    message.setProperty('sync.changeCount', (long) (delta.upserts.size() + delta.deleteIds.size()))
    message.setProperty('sync.hasChanges', (!delta.isFullLoadRequired && (delta.upserts || delta.deleteIds)).toString())

    def packages = delta.isFullLoadRequired ? [] :
            SyncLogic.buildApplyChangesPackages(delta.syncVersion, false, delta.upserts, delta.deleteIds, packageSize)
    message.setProperty('sync.packages', packages)
    message.setProperty('sync.packageIndex', 0)
    message.setProperty('sync.hasMorePackages', (!packages.isEmpty()).toString())
    message.setProperty('sync.mode', delta.isFullLoadRequired ? 'FULL_LOAD' : (packages ? 'DELTA' : 'NO_CHANGES'))
    addCustomHeader(message, 'Mode', message.getProperty('sync.mode') as String)
    addCustomHeader(message, 'NewVersion', delta.syncVersion.toString())
    message.setBody('')
    return message
}

/** Prepares the CSRF token request (GET service root). */
def Message prepareCsrfFetch(Message message) {
    message.setProperty('sync.step', 'Fetch CSRF token')
    message.setProperty('sync.requestPath', SyncLogic.normalizePath(message.getProperty('sync.servicePath') as String) + '/')
    clearHttpHeaders(message)
    message.setHeader('x-csrf-token', 'Fetch')
    message.setHeader('Accept', 'application/json')
    message.setBody('')
    return message
}

/** Keeps the CSRF token and the session cookies for the following POST requests. */
def Message storeCsrfToken(Message message) {
    def token = message.getHeader('x-csrf-token', String)
    if (!token || token.equalsIgnoreCase('Fetch') || token.equalsIgnoreCase('Required')) {
        throw new IllegalStateException('S/4HANA did not return a CSRF token (check user, service publication and client).')
    }
    message.setProperty('sync.csrfToken', token)
    message.setProperty('sync.cookie', SyncLogic.buildCookieHeader(message.getHeader('set-cookie', Object)))
    message.setBody('')
    return message
}

/** Takes the next prepared delta package. */
def Message nextDeltaPackage(Message message) {
    List<String> packages = message.getProperty('sync.packages') as List<String>
    int index = message.getProperty('sync.packageIndex') as int
    message.setProperty('sync.packageIndex', index + 1)
    message.setProperty('sync.hasMorePackages', (index + 1 < packages.size()).toString())
    message.setProperty('sync.step', "Apply delta package ${index + 1} of ${packages.size()}".toString())
    preparePost(message, 'applyChanges', packages[index])
    return message
}

/** Evaluates the per-record messages of an action response. Errors fail the run. */
def Message evaluateActionResponse(Message message) {
    def json = message.getBody(String)
    logPayload(message, "Response ${message.getProperty('sync.step')}", json)
    def outcome = SyncLogic.evaluateActionResult(json)
    if (outcome.errors) {
        throw new IllegalStateException("${message.getProperty('sync.step')} failed: " +
                outcome.errors.take(10).collect { SyncLogic.formatMessage(it) }.join(' | '))
    }

    long warnings = (message.getProperty('sync.warningCount') as long) + outcome.warnings.size()
    message.setProperty('sync.warningCount', warnings)
    List details = message.getProperty('sync.warningDetails') as List
    details.addAll(outcome.warnings.take(Math.max(0, SyncLogic.MAX_WARNING_DETAILS - details.size())))
    message.setProperty('sync.summary', SyncLogic.mergeSummary(message.getProperty('sync.summary') as String, outcome.summary))
    message.setBody('')
    return message
}

/** Starts the paged full load. */
def Message initFullLoad(Message message) {
    message.setProperty('sync.afterId', 0L)
    message.setProperty('sync.hasMorePages', 'true')
    message.setProperty('sync.pageCount', 0)
    message.setProperty('sync.sourceIds', new ArrayList<Long>(40000))
    return message
}

/** Builds the JDBC call for the next full-load page. */
def Message preparePageRequest(Message message) {
    long afterId = message.getProperty('sync.afterId') as long
    int pageSize = message.getProperty('sync.pageSize') as int
    int page = (message.getProperty('sync.pageCount') as int) + 1
    message.setProperty('sync.pageCount', page)
    message.setProperty('sync.step', "Read full-load page ${page} (after ID ${afterId})".toString())
    message.setBody(SyncLogic.buildProcedureCall('dbo.usp_GetMeasurementTaskPage', [
            [name: 'AfterId', type: 'INTEGER', value: afterId],
            [name: 'PageSize', type: 'INTEGER', value: pageSize]]))
    return message
}

/** Evaluates a full-load page and prepares its applyChanges request. */
def Message evaluatePage(Message message) {
    def json = SyncLogic.extractPayload(message.getBody(String))
    logPayload(message, "Page ${message.getProperty('sync.pageCount')}", json)
    def page = SyncLogic.parsePage(json, message.getProperty('sync.afterId') as long)

    List<Long> sourceIds = message.getProperty('sync.sourceIds') as List<Long>
    page.rows.each { sourceIds.add(it.id as Long) }
    message.setProperty('sync.afterId', page.lastId)
    message.setProperty('sync.hasMorePages', page.hasMore.toString())
    message.setProperty('sync.pageHasRows', (!page.rows.isEmpty()).toString())

    if (page.rows) {
        long version = message.getProperty('sync.newVersion') as long
        def body = SyncLogic.buildApplyChangesPackages(version, true, page.rows, [], page.rows.size())[0]
        message.setProperty('sync.step', "Apply full-load page ${message.getProperty('sync.pageCount')}".toString())
        preparePost(message, 'applyChanges', body)
    } else {
        message.setBody('')
    }
    return message
}

/** Builds the sweep request with all source IDs of the full load. */
def Message prepareFinalizeRequest(Message message) {
    long version = message.getProperty('sync.newVersion') as long
    List<Long> sourceIds = message.getProperty('sync.sourceIds') as List<Long>
    message.setProperty('sync.step', "Finalize full load (${sourceIds.size()} source IDs)".toString())
    addCustomHeader(message, 'SourceRows', sourceIds.size().toString())
    preparePost(message, 'finalizeFullLoad', SyncLogic.buildFinalizeBody(version, sourceIds))
    return message
}

/** Decides - after re-reading the Data Store - whether the watermark is written. */
def Message prepareWatermarkWrite(Message message) {
    def stored = SyncLogic.parseWatermark(message.getBody(String))
    long newVersion = message.getProperty('sync.newVersion') as long
    boolean fullLoad = (message.getProperty('sync.isFullLoad') as String) == 'true'
    boolean write = SyncLogic.shouldWriteWatermark(stored, newVersion, fullLoad)
    message.setProperty('sync.writeWatermark', write.toString())
    message.setProperty('sync.step', 'Write watermark')
    message.setBody(write ? SyncLogic.buildWatermarkEntry(newVersion, message.getProperty('sync.targetHost') as String,
            message.getProperty('sync.runId') as String, Instant.now().toString()) : '')
    return message
}

/** Writes the run summary to the message processing log. */
def Message logRunSummary(Message message) {
    addCustomHeader(message, 'WatermarkWritten', message.getProperty('sync.writeWatermark') as String)
    addCustomHeader(message, 'Summary', (message.getProperty('sync.summary') ?: 'no S/4 call') as String)
    addCustomHeader(message, 'Warnings', message.getProperty('sync.warningCount').toString())
    List details = message.getProperty('sync.warningDetails') as List
    details.eachWithIndex { warning, int i -> addCustomHeader(message, "Warning ${i + 1}", SyncLogic.formatMessage(warning)) }
    def log = messageLogFactory?.getMessageLog(message)
    if (log != null && details) {
        log.addAttachmentAsString('Warnings (first entries)', JsonOutput.prettyPrint(JsonOutput.toJson(details)), 'application/json')
    }
    return message
}

/** Exception subprocess: keeps the original error and builds the error handler request. */
def Message buildErrorContext(Message message) {
    def exception = message.getProperty('CamelExceptionCaught') as Throwable
    message.setProperty('sync.originalException', exception)

    def http = SyncLogic.extractHttpDetails(exception)
    def context = SyncLogic.buildErrorContext([
            interfaceId : message.getProperty('sync.interfaceId'),
            targetSystem: message.getProperty('sync.targetHost'),
            messageId   : message.getHeader('SAP_MessageProcessingLogID', String),
            runId       : message.getProperty('sync.runId'),
            step        : message.getProperty('sync.step'),
            exception   : exception,
            httpStatus  : http.statusCode,
            httpBody    : http.responseBody,
            mode        : message.getProperty('sync.mode'),
            lastVersion : message.getProperty('sync.lastVersion'),
            newVersion  : message.getProperty('sync.newVersion')])

    context.headers.each { String name, String value -> message.setHeader(name, value) }
    message.setBody(context.body)
    addCustomHeader(message, 'ErrorStep', message.getProperty('sync.step') as String)
    logPayload(message, 'Error context', context.body, true)
    return message
}

/** Error handler not reachable: log and continue - the original error is raised afterwards. */
def Message logErrorHandlerFailure(Message message) {
    def exception = message.getProperty('CamelExceptionCaught') as Throwable
    addCustomHeader(message, 'ErrorHandlerFailure', SyncLogic.truncate(exception?.toString() ?: 'unknown', 200))
    return message
}

/** Ends the exception subprocess with the original error so that the run fails. */
def Message rethrowOriginalError(Message message) {
    def exception = message.getProperty('sync.originalException') as Throwable
    if (exception instanceof Exception) {
        throw exception
    }
    throw new IllegalStateException("Synchronization failed in step '${message.getProperty('sync.step')}'", exception)
}

// ---------------------------------------------------------------------------
// Helpers with access to the message
// ---------------------------------------------------------------------------

private void preparePost(Message message, String action, String body) {
    message.setProperty('sync.requestPath', SyncLogic.buildActionPath(
            message.getProperty('sync.servicePath') as String,
            message.getProperty('sync.entitySet') as String,
            message.getProperty('sync.actionNamespace') as String,
            action))
    clearHttpHeaders(message)
    message.setHeader('Content-Type', 'application/json')
    message.setHeader('Accept', 'application/json')
    message.setHeader('x-csrf-token', message.getProperty('sync.csrfToken') as String)
    def cookie = message.getProperty('sync.cookie') as String
    if (cookie) {
        message.setHeader('Cookie', cookie)
    }
    logPayload(message, "Request ${message.getProperty('sync.step')}", body)
    message.setBody(body)
}

private void clearHttpHeaders(Message message) {
    ['x-csrf-token', 'Cookie', 'set-cookie', 'Content-Type', 'Accept', 'CamelHttpResponseCode',
     'CamelHttpResponseText', 'CamelHttpMethod', 'CamelHttpUri', 'CamelHttpPath', 'CamelHttpQuery'].each {
        message.getHeaders().remove(it)
    }
}

private void addCustomHeader(Message message, String name, String value) {
    def log = messageLogFactory?.getMessageLog(message)
    if (log != null && value != null) {
        log.addCustomHeaderProperty(name, SyncLogic.truncate(value, 200))
    }
}

private void logPayload(Message message, String name, String content, boolean always = false) {
    if (!always && (message.getProperty('sync.payloadLogging') as String)?.toLowerCase() != 'true') {
        return
    }
    def log = messageLogFactory?.getMessageLog(message)
    if (log != null && content != null) {
        log.addAttachmentAsString(name, content, name.startsWith('Page') || name.startsWith('Delta') ||
                content.startsWith('{') ? 'application/json' : 'text/plain')
    }
}

// ---------------------------------------------------------------------------
// Pure logic (unit-tested in cpi/test)
// ---------------------------------------------------------------------------

class SyncLogic {

    static final int MAX_WARNING_DETAILS = 20
    static final int MAX_ERROR_BODY_LENGTH = 4000
    static final String INTERFACE_ID = 'Frigibin_Measurement_Tasks_to_SAP'

    static String requireText(Object value, String name) {
        def text = value?.toString()?.trim()
        if (!text || text.startsWith('{{')) {
            throw new IllegalArgumentException("Configuration value '${name}' is missing.")
        }
        return text
    }

    static int toPositiveInt(Object value, String name, int min, int max) {
        def text = value?.toString()?.trim()
        if (!(text ==~ /\d{1,9}/)) {
            throw new IllegalArgumentException("Configuration value '${name}' must be a number, got '${text}'.")
        }
        int number = text as int
        if (number < min || number > max) {
            throw new IllegalArgumentException("Configuration value '${name}' must be between ${min} and ${max}, got ${number}.")
        }
        return number
    }

    /** Watermark entry of the Data Store: JSON {"version": n, ...}; missing or empty = -1. */
    static long parseWatermark(String body) {
        if (body == null || body.trim().isEmpty()) {
            return -1L
        }
        def json
        try {
            json = new JsonSlurper().parseText(body)
        } catch (Exception ignored) {
            throw new IllegalStateException('Watermark entry in the Data Store is not valid JSON - delete the entry to force a full load.')
        }
        return toVersion(json instanceof Map ? json.version : null, 'watermark version')
    }

    static long toVersion(Object value, String name) {
        if (value instanceof Number && !(value instanceof Double) && !(value instanceof Float)
                && !(value instanceof BigDecimal && ((BigDecimal) value).scale() > 0)) {
            return ((Number) value).longValue()
        }
        if (value instanceof CharSequence && value.toString() ==~ /-?\d{1,18}/) {
            return value.toString() as long
        }
        throw new IllegalStateException("Invalid ${name}: '${value}'.")
    }

    /** JDBC receiver, XML SQL format, stored procedure with typed input parameters (positional binding). */
    static String buildProcedureCall(String procedure, List<Map> parameters) {
        if (!(procedure ==~ /[A-Za-z0-9_.]+/)) {
            throw new IllegalArgumentException("Invalid procedure name '${procedure}'.")
        }
        def params = parameters.collect { Map p ->
            if (!(p.name ==~ /[A-Za-z][A-Za-z0-9_]*/) || !(p.type in ['BIGINT', 'INTEGER'])) {
                throw new IllegalArgumentException("Invalid parameter definition ${p}.")
            }
            long value = toVersion(p.value, p.name as String)
            if (p.type == 'INTEGER' && (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)) {
                throw new IllegalArgumentException("Parameter ${p.name} out of INTEGER range: ${value}.")
            }
            "<${p.name} isInput=\"true\" type=\"${p.type}\">${value}</${p.name}>"
        }.join('')
        return '<?xml version="1.0" encoding="UTF-8"?><root><StatementName><storedProcedureName action="EXECUTE">' +
                "<table>${procedure}</table>${params}</storedProcedureName></StatementName></root>"
    }

    /** Unwraps the JSON document from the JDBC response envelope (column "Payload"). */
    static String extractPayload(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalStateException('Empty response from the database.')
        }
        def trimmed = response.trim()
        if (trimmed.startsWith('{')) {
            return trimmed
        }
        def root = new XmlSlurper(false, false).parseText(trimmed)
        def payloads = root.depthFirst().findAll { it.name().equalsIgnoreCase('Payload') }
        if (payloads.isEmpty()) {
            throw new IllegalStateException('Database response does not contain a Payload column.')
        }
        // FOR JSON output longer than 2033 characters may be split into several rows: concatenate
        def json = payloads.collect { it.text() }.join('')
        if (!json.trim().startsWith('{')) {
            throw new IllegalStateException('Payload column does not contain a JSON object.')
        }
        return json.trim()
    }

    static Map parseDelta(String json) {
        def delta = new JsonSlurper().parseText(json) as Map
        if (!(delta.isFullLoadRequired instanceof Boolean) || !(delta.changes instanceof List)) {
            throw new IllegalStateException('Unexpected delta document structure.')
        }
        List<Map> upserts = []
        List<Long> deleteIds = []
        (delta.changes as List<Map>).each { Map change ->
            long id = toVersion(change.id, 'source id')
            switch (change.op) {
                case 'I':
                case 'U':
                    upserts << toUpsert(change)
                    break
                case 'D':
                    deleteIds << id
                    break
                default:
                    throw new IllegalStateException("Unknown change operation '${change.op}' for id ${id}.")
            }
        }
        return [syncVersion       : toVersion(delta.syncVersion, 'syncVersion'),
                isFullLoadRequired: delta.isFullLoadRequired,
                upserts           : upserts,
                deleteIds         : deleteIds]
    }

    static Map parsePage(String json, long afterId) {
        def page = new JsonSlurper().parseText(json) as Map
        if (!(page.hasMore instanceof Boolean) || !(page.rows instanceof List)) {
            throw new IllegalStateException('Unexpected page document structure.')
        }
        long lastId = toVersion(page.lastId, 'lastId')
        if (page.hasMore && lastId <= afterId) {
            throw new IllegalStateException("Full-load paging does not advance (after ${afterId}, last ${lastId}).")
        }
        return [lastId : lastId,
                hasMore: page.hasMore,
                rows   : (page.rows as List<Map>).collect { toUpsert(it) }]
    }

    /**
     * Source row without the change operation and without JSON null members:
     * the RAP action parameter does not accept null; an absent member is
     * received as initial value, which is the ABAP representation of NULL.
     */
    static Map toUpsert(Map row) {
        def result = new LinkedHashMap(row)
        result.remove('op')
        result.values().removeIf { it == null }
        return result
    }

    static List<String> buildApplyChangesPackages(long syncVersion, boolean isFullLoad, List<Map> upserts,
                                                  List<Long> deleteIds, int packageSize) {
        List<String> packages = []
        List<List> items = upserts.collect { ['U', it] } + deleteIds.collect { ['D', it] }
        if (packageSize < 1) {
            throw new IllegalArgumentException("Invalid package size ${packageSize}.")
        }
        items.collate(Math.min(packageSize, Math.max(1, items.size()))).each { List chunk ->
            packages << JsonOutput.toJson([
                    syncVersion: syncVersion,
                    isFullLoad : isFullLoad,
                    _Upserts   : chunk.findAll { it[0] == 'U' }.collect { it[1] },
                    _Deletes   : chunk.findAll { it[0] == 'D' }.collect { [id: it[1]] }])
        }
        return packages
    }

    static String buildFinalizeBody(long syncVersion, Collection<Long> sourceIds) {
        return JsonOutput.toJson([syncVersion: syncVersion, _SourceIds: sourceIds.collect { [id: it] }])
    }

    /** OData V4 action result {"value": [ {measurementTask, severity, fieldName, fieldValue, messageText} ]}. */
    static Map evaluateActionResult(String json) {
        List<Map> messages = []
        if (json != null && !json.trim().isEmpty()) {
            def result = new JsonSlurper().parseText(json)
            if (result instanceof Map && result.value instanceof List) {
                messages = result.value as List<Map>
            } else if (result instanceof List) {
                messages = result as List<Map>
            } else {
                throw new IllegalStateException('Unexpected action response structure.')
            }
        }
        return [errors  : messages.findAll { it.severity in ['E', 'A', 'X'] },
                warnings: messages.findAll { it.severity == 'W' },
                summary : messages.findAll { it.severity == 'I' && !it.measurementTask }.collect { it.messageText }.join('; ')]
    }

    static String mergeSummary(String existing, String addition) {
        if (!addition) {
            return existing
        }
        return truncate(existing ? "${existing} / ${addition}".toString() : addition, 1000)
    }

    static String formatMessage(Map message) {
        def parts = []
        if (message.measurementTask) parts << "#${message.measurementTask}"
        if (message.fieldName) parts << "${message.fieldName}=${message.fieldValue ?: ''}"
        parts << (message.messageText ?: '')
        return truncate(parts.join(' '), 200)
    }

    static boolean shouldWriteWatermark(long stored, long newVersion, boolean fullLoad) {
        if (newVersion < 0) {
            return false
        }
        // After a full load the version is written even if it is lower (database restore)
        return fullLoad ? newVersion != stored : newVersion > stored
    }

    static String buildWatermarkEntry(long version, String targetHost, String runId, String timestamp) {
        return JsonOutput.toJson([version: version, targetHost: targetHost, runId: runId, writtenAt: timestamp,
                                  interfaceId: INTERFACE_ID])
    }

    static String buildCookieHeader(Object setCookie) {
        List<String> values
        if (setCookie == null) {
            values = []
        } else if (setCookie instanceof Collection) {
            values = setCookie.collect { it.toString() }
        } else {
            // Several Set-Cookie headers may arrive joined with a comma; split only before "name="
            values = setCookie.toString().split(/,(?=\s*[^;,=\s]+=)/) as List<String>
        }
        return values.collect { it.split(';')[0].trim() }.findAll { it.contains('=') }.join('; ')
    }

    static String normalizePath(String path) {
        def text = requireText(path, 'service path')
        text = text.startsWith('/') ? text : '/' + text
        return text.endsWith('/') ? text[0..-2] : text
    }

    static String buildActionPath(String servicePath, String entitySet, String namespace, String action) {
        return "${normalizePath(servicePath)}/${requireText(entitySet, 'entity set')}/" +
                "${requireText(namespace, 'action namespace')}.${action}"
    }

    static Map extractHttpDetails(Throwable exception) {
        def current = exception
        while (current != null) {
            def status = invoke(current, 'getStatusCode')
            if (status != null) {
                return [statusCode  : status.toString(),
                        responseBody: truncate(invoke(current, 'getResponseBody')?.toString(), MAX_ERROR_BODY_LENGTH)]
            }
            current = current.cause == current ? null : current.cause
        }
        return [statusCode: null, responseBody: null]
    }

    static Map buildErrorContext(Map input) {
        Throwable exception = input.exception as Throwable
        String text = truncate(extractODataErrorText(input.httpBody as String) ?:
                (exception?.message ?: exception?.toString() ?: 'unknown error'), 1000)
        def body = JsonOutput.toJson([
                interfaceId : input.interfaceId ?: INTERFACE_ID,
                targetSystem: input.targetSystem,
                messageId   : input.messageId,
                runId       : input.runId,
                step        : input.step,
                severity    : 'ERROR',
                text        : text,
                timestamp   : Instant.now().toString(),
                details     : [exceptionClass  : exception?.class?.name,
                               exceptionMessage: truncate(exception?.message, MAX_ERROR_BODY_LENGTH),
                               httpStatusCode  : input.httpStatus,
                               httpResponseBody: input.httpBody,
                               mode            : input.mode,
                               lastVersion     : input.lastVersion,
                               newVersion      : input.newVersion]])
        def headers = [
                'ic.error.interfaceId' : (input.interfaceId ?: INTERFACE_ID) as String,
                'ic.error.targetSystem': input.targetSystem as String,
                'ic.error.messageId'   : input.messageId as String,
                'ic.error.runId'       : input.runId as String,
                'ic.error.step'        : input.step as String,
                'ic.error.severity'    : 'ERROR',
                'ic.error.text'        : text,
                'Content-Type'         : 'application/json']
        return [body: body, headers: headers.findAll { it.value != null }]
    }

    static String extractODataErrorText(String body) {
        if (!body?.trim()?.startsWith('{')) {
            return null
        }
        try {
            def json = new JsonSlurper().parseText(body)
            def error = json instanceof Map ? json.error : null
            if (!(error instanceof Map)) {
                return null
            }
            def texts = [error.message]
            if (error.details instanceof List) {
                texts.addAll((error.details as List<Map>).collect { it.message })
            }
            return texts.findAll { it }.unique().join(' | ')
        } catch (Exception ignored) {
            return null
        }
    }

    static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value
        }
        return value.substring(0, max - 3) + '...'
    }

    private static Object invoke(Object target, String method) {
        try {
            def m = target.class.methods.find { it.name == method && it.parameterCount == 0 }
            return m?.invoke(target)
        } catch (Exception ignored) {
            return null
        }
    }
}
