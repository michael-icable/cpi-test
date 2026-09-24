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
						
				<xsl:variable name="homeAddress"
					select="//wd:Address_Data[wd:Usage_Data/wd:Type_Data/wd:Type_Reference/wd:ID[@wd:type='Communication_Usage_Type_ID']='HOME']"/>
				
                <Insert_Employees>
                    <dbTableName action="UPDATE_INSERT">
                        <table>stage.wd_md_employees</table>
                        <access>
							<OrganizationID>
								<xsl:value-of select="$job/wd:Position_Data/wd:Business_Site_Summary_Data/wd:Location_Reference/wd:ID[@wd:type='Location_ID']"/>
							</OrganizationID>
                            <WID>
                                <xsl:value-of select="wd:Worker_Reference/wd:ID[@wd:type='WID']"/>
                            </WID>

                            <Employee_ID>
                                <xsl:value-of select="wd:Worker_Reference/wd:ID[@wd:type='Employee_ID']"/>
                            </Employee_ID>

                            <User_ID>
                                <xsl:value-of select="wd:Worker_Data/wd:User_ID"/>
                            </User_ID>

                            <Universal_ID>
                                <xsl:value-of select="wd:Worker_Data/wd:Universal_ID"/>
                            </Universal_ID>

                            <First_Name>
                                <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:First_Name"/>
                            </First_Name>

                            <Last_Name>
                                <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:Last_Name"/>
                            </Last_Name>

                            <Secondary_Last_Name>
                                <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Name_Data/wd:Legal_Name_Data/wd:Name_Detail_Data/wd:Secondary_Last_Name"/>
                            </Secondary_Last_Name>

                            <Gender_Code>
                                <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Gender_Reference/wd:ID[@wd:type='Gender_Code']"/>
                            </Gender_Code>
							
                            <Legacy_ID>
                                <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Identification_Data/wd:Custom_ID/wd:Custom_ID_Data/wd:ID"/>
                            </Legacy_ID>
							
                            <Municipality>
                                <xsl:value-of select="$homeAddress/wd:Municipality"/>
                            </Municipality>
							
                            <Postal_Code>
                                <xsl:value-of select="$homeAddress/wd:Postal_Code"/>
                            </Postal_Code>
							
							<Country_Region_ID>
                                <xsl:value-of select="$homeAddress/wd:Country_Region_Reference/wd:ISO_3166-2_Country-Region_Code"/>
                            </Country_Region_ID>
							
                            <Country_Region_Descriptor>
                                <xsl:value-of select="$homeAddress/wd:Country_Region_Descriptor"/>
                            </Country_Region_Descriptor>
							
                            <Address_Line_Data>
                                <xsl:value-of select="$homeAddress/wd:Address_Line_Data"/>
                            </Address_Line_Data>
							
                            <Email_Address_Work>
                                <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Contact_Data/wd:Email_Address_Data/wd:Email_Address"/>
                            </Email_Address_Work>

                            <Employee_Type_ID>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Job_Data/wd:Position_Data/wd:Worker_Type_Reference/wd:ID[@wd:type='Employee_Type_ID']"/>
                            </Employee_Type_ID>

                            <Position_Time_Type_ID>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Job_Data/wd:Position_Data/wd:Position_Time_Type_Reference/wd:ID[@wd:type='Position_Time_Type_ID']"/>
                            </Position_Time_Type_ID>

                            <Scheduled_Weekly_Hours>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Job_Data/wd:Position_Data/wd:Scheduled_Weekly_Hours"/>
                            </Scheduled_Weekly_Hours>

                            <Full_Time_Equivalent_Percentage>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Job_Data/wd:Position_Data/wd:Full_Time_Equivalent_Percentage"/>
                            </Full_Time_Equivalent_Percentage>

                            <Exclude_from_Headcount>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Job_Data/wd:Position_Data/wd:Exclude_from_Headcount"/>
                            </Exclude_from_Headcount>

                            <Pay_Rate_Type_ID>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Job_Data/wd:Position_Data/wd:Pay_Rate_Type_Reference/wd:ID[@wd:type='Pay_Rate_Type_ID']"/>
                            </Pay_Rate_Type_ID>

                            <Position_Title>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Job_Data/wd:Position_Data/wd:Position_Title"/>
                            </Position_Title>
							
                            <Manager_ID>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Job_Data/wd:Position_Data/wd:Manager_as_of_last_detected_manager_change_Reference/wd:ID[@wd:type='Employee_ID']"/>
                            </Manager_ID>

                            <Active>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Active"/>
                            </Active>

                            <Active_Status_Date>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Active_Status_Date"/>
                            </Active_Status_Date>

                            <Hire_Date>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Hire_Date"/>
                            </Hire_Date>

                            <Original_Hire_Date>
                                <xsl:value-of select="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Original_Hire_Date"/>
                            </Original_Hire_Date>

                            <Worker_Status>
                                <xsl:choose>
                                    <xsl:when test="wd:Worker_Data/wd:Employment_Data/wd:Worker_Status_Data/wd:Active='1'">ACTIVE</xsl:when>
                                    <xsl:otherwise>INACTIVE</xsl:otherwise>
                                </xsl:choose>
                            </Worker_Status>

                            <User_Name>
                                <xsl:value-of select="wd:Worker_Data/wd:User_Account_Data/wd:User_Name"/>
                            </User_Name>

                            <Email_Address>
                                <xsl:value-of select="wd:Worker_Data/wd:Personal_Data/wd:Contact_Data/wd:Email_Address_Data/wd:Email_Address"/>
                            </Email_Address>

                        </access>

                        <key>
                            <WID>
                                <xsl:value-of select="wd:Worker_Reference/wd:ID[@wd:type='WID']"/>
                            </WID>
                        </key>

                    </dbTableName>
                </Insert_Employees>
            </xsl:for-each>
		</Workers>
    </xsl:template>
</xsl:stylesheet>