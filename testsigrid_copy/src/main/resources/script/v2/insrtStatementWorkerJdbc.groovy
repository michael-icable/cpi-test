// This is Groovy Flowstep Version 2.x, running with Groovy runtime 4, Downgrade the script if older behaviour needed.

package script.v2

import com.sap.it.script.v2.api.Message
import groovy.xml.XmlSlurper

private static String esc(final String value) {

    if (value == null || value.trim().isEmpty()) {
        return "NULL"
    }

    return "'" + value.trim().replace("'", "''") + "'"
}
    
    def Message processData(Message message) {
    final def xml = new XmlSlurper().parse(
        message.getBody(java.io.Reader)
    )

    final StringBuilder sql = new StringBuilder()
    
    xml.Worker.each { Object w ->

        sql.append("""
        MERGE dbo.WORKERS AS TARGET
        USING (
            SELECT ${esc(w.Employee_ID.text())} EMPLOYEE_ID
        ) AS SOURCE
        ON TARGET.EMPLOYEE_ID = SOURCE.EMPLOYEE_ID

        WHEN MATCHED THEN
        UPDATE SET
            UNIVERSAL_ID = ${esc(w.Universal_ID.text())},
            USER_ID = ${esc(w.User_ID.text())},
            USER_NAME = ${esc(w.User_Name.text())},
            FIRST_NAME = ${esc(w.First_Name.text())},
            LAST_NAME = ${esc(w.Last_Name.text())},
            BIRTH_DATE = ${esc(w.Birth_Date.text())},
            GENDER_CODE = ${esc(w.Gender_Code.text())},
            EMAIL_ADDRESS = ${esc(w.Email_Address.text())},
            ACTIVE = ${w.Active.text() ?: '0'},
            HIRE_DATE = ${esc(w.Hire_Date.text())},
            POSITION_TIME_TYPE_ID = ${esc(w.Position_Time_Type_ID.text())},
            Organization_ID = ${esc(w.Organization_ID.text())},
            MANAGER_EMPLOYEE_ID = ${esc(w.Manager_Employee_ID.text())},
            Manager_Worker_Description = ${esc(w.Manager_Worker_Description.text())}

        WHEN NOT MATCHED THEN
        INSERT (
            EMPLOYEE_ID,
            UNIVERSAL_ID,
            USER_ID,
            USER_NAME,
            FIRST_NAME,
            LAST_NAME,
            BIRTH_DATE,
            GENDER_CODE,
            EMAIL_ADDRESS,
            ACTIVE,
            HIRE_DATE,
            POSITION_TIME_TYPE_ID,
            Organization_ID,
            MANAGER_EMPLOYEE_ID,
            Manager_Worker_Description
        )
        VALUES (
            ${esc(w.Employee_ID.text())},
            ${esc(w.Universal_ID.text())},
            ${esc(w.User_ID.text())},
            ${esc(w.User_Name.text())},
            ${esc(w.First_Name.text())},
            ${esc(w.Last_Name.text())},
            ${esc(w.Birth_Date.text())},
            ${esc(w.Gender_Code.text())},
            ${esc(w.Email_Address.text())},
            ${w.Active.text() ?: '0'},
            ${esc(w.Hire_Date.text())},
            ${esc(w.Position_Time_Type_ID.text())},
            ${esc(w.Organization_ID.text())},
            ${esc(w.Manager_Employee_ID.text())},
            ${esc(w.Manager_Worker_Description.text())}
        );

        """)
    }

    message.setBody(sql.toString())

    return message
}