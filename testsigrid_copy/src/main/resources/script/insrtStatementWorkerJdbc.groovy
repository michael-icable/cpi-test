import com.sap.gateway.ip.core.customdev.util.Message
import groovy.util.XmlSlurper

private static String esc(String value) {
        value = value?.trim()
    
        if (!value) {
            return "NULL"
        }
    
        return "'" + value.replace("'", "''") + "'"
    }
    
    def Message processData(Message message) {
    def xml = new XmlSlurper().parse(message.getBody(java.io.Reader))

    def sql = new StringBuilder()
    
    xml.Worker.each { w ->

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
            MIDDLE_NAME = ${esc(w.Middle_Name.text())},
            LAST_NAME = ${esc(w.Last_Name.text())},
            SECOND_LAST_NAME = ${esc(w.Second_Last_Name.text())},
            BIRTH_DATE = ${esc(w.Birth_Date.text())},
            GENDER = ${esc(w.Gender.text())},
            EMAIL = ${esc(w.Email.text())},
            ACTIVE = ${w.Active.text() ?: '0'},
            HIRE_DATE = ${esc(w.Hire_Date.text())},
            POSITION_ID = ${esc(w.Position_ID.text())},
            POSITION_TITLE = ${esc(w.Position_Title.text())},
            LOCATION_ID = ${esc(w.Location_ID.text())},
            MANAGER_EMPLOYEE_ID = ${esc(w.Manager_Employee_ID.text())},
            TOP_MANAGER_NAME = ${esc(w.Top_Manager_Name.text())}

        WHEN NOT MATCHED THEN
        INSERT (
            EMPLOYEE_ID,
            UNIVERSAL_ID,
            USER_ID,
            USER_NAME,
            FIRST_NAME,
            MIDDLE_NAME,
            LAST_NAME,
            SECOND_LAST_NAME,
            BIRTH_DATE,
            GENDER,
            EMAIL,
            ACTIVE,
            HIRE_DATE,
            POSITION_ID,
            POSITION_TITLE,
            LOCATION_ID,
            MANAGER_EMPLOYEE_ID,
            TOP_MANAGER_NAME
        )
        VALUES (
            ${esc(w.Employee_ID.text())},
            ${esc(w.Universal_ID.text())},
            ${esc(w.User_ID.text())},
            ${esc(w.User_Name.text())},
            ${esc(w.First_Name.text())},
            ${esc(w.Middle_Name.text())},
            ${esc(w.Last_Name.text())},
            ${esc(w.Second_Last_Name.text())},
            ${esc(w.Birth_Date.text())},
            ${esc(w.Gender.text())},
            ${esc(w.Email.text())},
            ${w.Active.text() ?: '0'},
            ${esc(w.Hire_Date.text())},
            ${esc(w.Position_ID.text())},
            ${esc(w.Position_Title.text())},
            ${esc(w.Location_ID.text())},
            ${esc(w.Manager_Employee_ID.text())},
            ${esc(w.Top_Manager_Name.text())}
        );

        """)
    }

    message.setBody(sql.toString())

    return message
}