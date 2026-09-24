<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:wd="urn:com.workday/bsvc"
    xmlns:env="http://schemas.xmlsoap.org/soap/envelope/"
    exclude-result-prefixes="wd env">

    <xsl:output method="xml" indent="yes"/>

   <xsl:template match="/">
    <Workers>

        <xsl:for-each select="//wd:Worker">

            <xsl:variable name="job"
                select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Job_Data[@wd:Primary_Job='1']"/>

            <Worker>

                <Employee_ID>
                    <xsl:value-of select="wd:Worker_Data/wd:Worker_ID"/>
                </Employee_ID>

                <Universal_ID>
                    <xsl:value-of select="wd:Worker_Data/wd:Universal_ID"/>
                </Universal_ID>

                <User_ID>
                    <xsl:value-of select="wd:Worker_Data/wd:User_ID"/>
                </User_ID>

                <User_Name>
                    <xsl:value-of select="wd:Worker_Data/wd:User_Account_Data/wd:User_Name"/>
                </User_Name>

                <First_Name>
                    <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:First_Name"/>
                </First_Name>

                <Middle_Name>
                    <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:Middle_Name"/>
                </Middle_Name>

                <Last_Name>
                    <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:Last_Name"/>
                </Last_Name>

                <Second_Last_Name>
                    <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:Secondary_Last_Name"/>
                </Second_Last_Name>

                <Birth_Date>
                    <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Personal_Information_Data/wd:Birth_Date"/>
                </Birth_Date>

                <Gender>
                    <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Personal_Information_Data/wd:Personal_Information_For_Country_Data/wd:Country_Personal_Information_Data/wd:Gender_Reference/wd:ID[@wd:type='Gender_Code']"/>
                </Gender>

                <Email>
                    <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Contact_Data/wd:Email_Address_Data[1]/wd:Email_Address"/>
                </Email>

                <Active>
                    <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Active"/>
                </Active>

                <Hire_Date>
                    <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Hire_Date"/>
                </Hire_Date>

                <Position_ID>
                    <xsl:value-of select="$job/wd:Position_Data/wd:Position_ID"/>
                </Position_ID>

                <Position_Title>
                    <xsl:value-of select="$job/wd:Position_Data/wd:Position_Title"/>
                </Position_Title>

                <Location_ID>
                    <xsl:value-of select="$job/wd:Position_Data/wd:Business_Site_Summary_Data/wd:Location_Reference/wd:ID[@wd:type='Location_ID']"/>
                </Location_ID>

                <Manager_Employee_ID>
                    <xsl:value-of select="$job/wd:Position_Data/wd:Manager_as_of_last_detected_manager_change_Reference/wd:ID[@wd:type='Employee_ID']"/>
                </Manager_Employee_ID>

                <Top_Manager_Name>
                    <xsl:value-of select="wd:Management_Chain_Data/wd:Worker_Supervisory_Management_Chain_Data/wd:Management_Chain_Data[last()]/wd:Manager/wd:Worker_Descriptor"/>
                </Top_Manager_Name>

            </Worker>

        </xsl:for-each>

    </Workers>
</xsl:template>

</xsl:stylesheet>