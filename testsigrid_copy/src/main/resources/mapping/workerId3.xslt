<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:wd="urn:com.workday/bsvc"
    xmlns:env="http://schemas.xmlsoap.org/soap/envelope/"
    exclude-result-prefixes="wd env">

    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="/">

        <!-- CSV Header -->
        <xsl:text>Employee_ID;</xsl:text>
        <xsl:text>Universal_ID;</xsl:text>
        <xsl:text>User_ID;</xsl:text>
        <xsl:text>User_Name;</xsl:text>
        <xsl:text>First_Name;</xsl:text>
        <xsl:text>Middle_Name;</xsl:text>
        <xsl:text>Last_Name;</xsl:text>
        <xsl:text>Second_Last_Name;</xsl:text>
        <xsl:text>Birth_Date;</xsl:text>
        <xsl:text>Gender;</xsl:text>
        <xsl:text>Marital_Status;</xsl:text>
        <xsl:text>Email;</xsl:text>
        <xsl:text>Phone;</xsl:text>
        <xsl:text>Home_Address;</xsl:text>
        <xsl:text>Home_City;</xsl:text>
        <xsl:text>Home_State;</xsl:text>
        <xsl:text>Home_Zip;</xsl:text>
        <xsl:text>Work_Address;</xsl:text>
        <xsl:text>Work_City;</xsl:text>
        <xsl:text>Work_State;</xsl:text>
        <xsl:text>Work_Zip;</xsl:text>
        <xsl:text>Active;</xsl:text>
        <xsl:text>Hire_Date;</xsl:text>
        <xsl:text>Original_Hire_Date;</xsl:text>
        <xsl:text>Seniority_Date;</xsl:text>
        <xsl:text>Position_ID;</xsl:text>
        <xsl:text>Position_Title;</xsl:text>
        <xsl:text>Business_Title;</xsl:text>
        <xsl:text>Worker_Type;</xsl:text>
        <xsl:text>Position_Time_Type;</xsl:text>
        <xsl:text>Scheduled_Weekly_Hours;</xsl:text>
        <xsl:text>FTE;</xsl:text>
        <xsl:text>Pay_Rate_Type;</xsl:text>
        <xsl:text>Job_Profile_ID;</xsl:text>
        <xsl:text>Job_Profile_Name;</xsl:text>
        <xsl:text>Management_Level;</xsl:text>
        <xsl:text>Job_Family;</xsl:text>
        <xsl:text>Location_ID;</xsl:text>
        <xsl:text>Location_Name;</xsl:text>
        <xsl:text>Country;</xsl:text>
        <xsl:text>Payroll_Frequency;</xsl:text>
        <xsl:text>Manager_Employee_ID;</xsl:text>
        <xsl:text>Top_Manager_Name</xsl:text>
        <xsl:text>&#10;</xsl:text>

        <xsl:for-each select="//wd:Worker">

            <xsl:variable name="job"
                select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Job_Data[@wd:Primary_Job='1']"/>

            <xsl:variable name="homeAddress"
                select="wd:Worker_Data/wd:Personal_Data/wd:Contact_Data/wd:Address_Data[wd:Usage_Data/wd:Type_Data/wd:Type_Reference/wd:ID='HOME'][1]"/>

            <xsl:variable name="workAddress"
                select="wd:Worker_Data/wd:Personal_Data/wd:Contact_Data/wd:Address_Data[wd:Usage_Data/wd:Type_Data/wd:Type_Reference/wd:ID='WORK'][1]"/>

            <!-- Employee -->
            <xsl:value-of select="wd:Worker_Data/wd:Worker_ID"/><xsl:text>;</xsl:text>
            <xsl:value-of select="wd:Worker_Data/wd:Universal_ID"/><xsl:text>;</xsl:text>
            <xsl:value-of select="wd:Worker_Data/wd:User_ID"/><xsl:text>;</xsl:text>
            <xsl:value-of select="wd:Worker_Data/wd:User_Account_Data/wd:User_Name"/><xsl:text>;</xsl:text>

            <!-- Name -->
            <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:First_Name"/><xsl:text>;</xsl:text>
            <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:Middle_Name"/><xsl:text>;</xsl:text>
            <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:Last_Name"/><xsl:text>;</xsl:text>
            <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:Secondary_Last_Name"/><xsl:text>;</xsl:text>

            <!-- Personal -->
            <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Personal_Information_Data/wd:Birth_Date"/><xsl:text>;</xsl:text>

            <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Personal_Information_Data/wd:Personal_Information_For_Country_Data/wd:Country_Personal_Information_Data/wd:Gender_Reference/wd:ID[@wd:type='Gender_Code']"/><xsl:text>;</xsl:text>

            <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Personal_Information_Data/wd:Personal_Information_For_Country_Data/wd:Country_Personal_Information_Data/wd:Marital_Status_Reference/wd:ID[2]"/><xsl:text>;</xsl:text>

            <!-- Contact -->
            <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Contact_Data/wd:Email_Address_Data[1]/wd:Email_Address"/><xsl:text>;</xsl:text>

            <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Contact_Data/wd:Phone_Data[1]/@wd:E164_Formatted_Phone"/><xsl:text>;</xsl:text>

            <!-- Home Address -->
            <xsl:value-of select="$homeAddress/wd:Address_Line_Data"/><xsl:text>;</xsl:text>
            <xsl:value-of select="$homeAddress/wd:Municipality"/><xsl:text>;</xsl:text>
            <xsl:value-of select="$homeAddress/wd:Country_Region_Descriptor"/><xsl:text>;</xsl:text>
            <xsl:value-of select="$homeAddress/wd:Postal_Code"/><xsl:text>;</xsl:text>

            <!-- Work Address -->
            <xsl:value-of select="$workAddress/wd:Address_Line_Data"/><xsl:text>;</xsl:text>
            <xsl:value-of select="$workAddress/wd:Municipality"/><xsl:text>;</xsl:text>
            <xsl:value-of select="$workAddress/wd:Country_Region_Descriptor"/><xsl:text>;</xsl:text>
            <xsl:value-of select="$workAddress/wd:Postal_Code"/><xsl:text>;</xsl:text>

            <!-- Status -->
            <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Active"/><xsl:text>;</xsl:text>
            <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Hire_Date"/><xsl:text>;</xsl:text>
            <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Original_Hire_Date"/><xsl:text>;</xsl:text>
            <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Seniority_Date"/><xsl:text>;</xsl:text>

            <!-- Position -->
            <xsl:value-of select="$job/wd:Position_Data/wd:Position_ID"/><xsl:text>;</xsl:text>
            <xsl:value-of select="$job/wd:Position_Data/wd:Position_Title"/><xsl:text>;</xsl:text>
            <xsl:value-of select="$job/wd:Position_Data/wd:Business_Title"/><xsl:text>;</xsl:text>

            <xsl:value-of select="$job/wd:Position_Data/wd:Worker_Type_Reference/wd:ID[@wd:type='Employee_Type_ID']"/><xsl:text>;</xsl:text>

            <xsl:value-of select="$job/wd:Position_Data/wd:Position_Time_Type_Reference/wd:ID[@wd:type='Position_Time_Type_ID']"/><xsl:text>;</xsl:text>

            <xsl:value-of select="$job/wd:Position_Data/wd:Scheduled_Weekly_Hours"/><xsl:text>;</xsl:text>

            <xsl:value-of select="$job/wd:Position_Data/wd:Full_Time_Equivalent_Percentage"/><xsl:text>;</xsl:text>

            <xsl:value-of select="$job/wd:Position_Data/wd:Pay_Rate_Type_Reference/wd:ID[@wd:type='Pay_Rate_Type_ID']"/><xsl:text>;</xsl:text>

            <!-- Job Profile -->
            <xsl:value-of select="$job/wd:Position_Data/wd:Job_Profile_Summary_Data/wd:Job_Profile_Reference/wd:ID[@wd:type='Job_Profile_ID']"/><xsl:text>;</xsl:text>

            <xsl:value-of select="$job/wd:Position_Data/wd:Job_Profile_Summary_Data/wd:Job_Profile_Name"/><xsl:text>;</xsl:text>

            <xsl:value-of select="$job/wd:Position_Data/wd:Job_Profile_Summary_Data/wd:Management_Level_Reference/wd:ID[@wd:type='Management_Level_ID']"/><xsl:text>;</xsl:text>

            <xsl:value-of select="$job/wd:Position_Data/wd:Job_Profile_Summary_Data/wd:Job_Family_Reference/wd:ID[@wd:type='Job_Family_ID']"/><xsl:text>;</xsl:text>

            <!-- Location -->
            <xsl:value-of select="$job/wd:Position_Data/wd:Business_Site_Summary_Data/wd:Location_Reference/wd:ID[@wd:type='Location_ID']"/><xsl:text>;</xsl:text>

            <xsl:value-of select="$job/wd:Position_Data/wd:Business_Site_Summary_Data/wd:Name"/><xsl:text>;</xsl:text>

            <xsl:value-of select="$job/wd:Position_Data/wd:Business_Site_Summary_Data/wd:Address_Data/wd:Country_Reference/wd:ID[@wd:type='ISO_3166-1_Alpha-2_Code']"/><xsl:text>;</xsl:text>

            <!-- Payroll -->
            <xsl:value-of select="$job/wd:Position_Data/wd:Payroll_Interface_Processing_Data/wd:Frequency_Reference/wd:ID[@wd:type='Frequency_ID']"/><xsl:text>;</xsl:text>

            <!-- Manager -->
            <xsl:value-of select="$job/wd:Position_Data/wd:Manager_as_of_last_detected_manager_change_Reference/wd:ID[@wd:type='Employee_ID']"/><xsl:text>;</xsl:text>

            <!-- Letzter Manager der Chain = oberster Manager -->
            <xsl:value-of select="wd:Management_Chain_Data/wd:Worker_Supervisory_Management_Chain_Data/wd:Management_Chain_Data[last()]/wd:Manager/wd:Worker_Descriptor"/>

            <xsl:text>&#10;</xsl:text>

        </xsl:for-each>

    </xsl:template>

</xsl:stylesheet>