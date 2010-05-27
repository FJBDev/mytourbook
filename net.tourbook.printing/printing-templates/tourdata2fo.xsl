<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" exclude-result-prefixes="fo">
	<xsl:output method="xml" version="1.0" omit-xml-declaration="no" indent="yes" />
	<!-- parameters passed from java code into the TransFormer, usefull for pre-formatting data in java or configuring i18n -->
	<xsl:param name="startDate" select="''" />	
	<xsl:param name="unitAltitude" select="''" />
	<xsl:param name="unitDistance" select="''" />
	<xsl:param name="unitTemperature" select="''" />			
	<xsl:param name="unitLabelDistance" select="''" />
	<xsl:param name="unitLabelSpeed" select="''" />
	<xsl:param name="unitLabelAltitude" select="''" />
	<xsl:param name="unitLabelTemperature" select="''" />
	<xsl:param name="unitLabelHeartBeat" select="''" />
	<xsl:param name="tourTime" select="''" />
	<xsl:param name="tourBreakTime" select="''" />
	<xsl:param name="tourDrivingTime" select="''" />
			
	<!-- ========================= -->
	<!-- root element: TourData -->
	<!-- ========================= -->
	<xsl:template match="TourData">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<fo:simple-page-master master-name="simpleA4" page-height="29.7cm" page-width="21cm" margin-top="2cm" margin-bottom="2cm" margin-left="2cm" margin-right="2cm">
					<fo:region-body />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:page-sequence master-reference="simpleA4">
				<fo:flow flow-name="xsl-region-body">

				<fo:block border-color="black" border-style="solid" border-width=".3mm"  margin-bottom="5pt" font-size="16pt" font-weight="bold" background-color="#E1E1E1" text-align="center">
					Tour: <xsl:value-of select="tourTitle" />
				</fo:block>
				
				
				<fo:block margin-bottom="5pt" font-size="10pt">
					<fo:table border-style="solid" border-width="0.5pt" border-color="black" table-layout="fixed" width="100%">
						<fo:table-column column-width="4cm" />
						<fo:table-column column-width="8cm" />
						<fo:table-header>
	    					<fo:table-row>
	    						<fo:table-cell border-style="solid" border-width="0.5pt" border-color="black" text-align="center" vertical-align="middle" number-columns-spanned="2" background-color="#E1E1E1">
	    							<fo:block text-align="center" vertical-align="middle">
	    								Tour / Event
	    							</fo:block>
	    						</fo:table-cell>	
	    					</fo:table-row>
	    				</fo:table-header>
						<fo:table-body>
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" number-columns-spanned="2" padding="2pt">
									<fo:block text-align="left" vertical-align="top" white-space-collapse="false" linefeed-treatment="preserve"><xsl:value-of select="tourDescription" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour start date -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Start:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="$startDate" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour start location -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Start location:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="tourStartPlace" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour end location -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">End location:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="tourEndPlace" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-body>
					</fo:table>
				</fo:block>
				
				
				<fo:block padding="0pt" margin-bottom="5pt" font-size="10pt">
					<fo:table border-style="solid" border-width="0.5pt" border-color="black" table-layout="fixed" width="100%">
						<fo:table-column column-width="4cm" />
						<fo:table-column column-width="8cm" />
						<fo:table-header>
	    					<fo:table-row>
	    						<fo:table-cell border-style="solid" border-width="0.5pt" border-color="black" text-align="center" vertical-align="middle" number-columns-spanned="2" background-color="#E1E1E1">
	    							<fo:block text-align="center" vertical-align="middle">
	    								Time - Distance - Speed
	    							</fo:block>
	    						</fo:table-cell>	
	    					</fo:table-row>
	    				</fo:table-header>
						<fo:table-body>
							<!-- tour time -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Tour time:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="$tourTime" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour moving time -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Tour pausing time:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="$tourBreakTime" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour moving time -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Tour moving time:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="$tourDrivingTime" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour distance -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Distance:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left" white-space-collapse="false" white-space="pre"><xsl:value-of select="format-number(tourDistance div 1000 div $unitDistance, '#.##')" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelDistance" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- maximum speed -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Maximum speed:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="format-number(maxSpeed div $unitDistance, '#.##')" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelSpeed" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-body>
					</fo:table>
				</fo:block>
				
				
				<fo:block padding="0pt" margin-bottom="5pt" font-size="10pt">
					<fo:table border-style="solid" border-width="0.5pt" border-color="black" table-layout="fixed" width="100%">
						<fo:table-column column-width="4cm" />
						<fo:table-column column-width="8cm" />
						<fo:table-header>
	    					<fo:table-row>
	    						<fo:table-cell border-style="solid" border-width="0.5pt" border-color="black" text-align="center" vertical-align="middle" number-columns-spanned="2" background-color="#E1E1E1">
	    							<fo:block text-align="center" vertical-align="middle">
	    								Personal
	    							</fo:block>
	    						</fo:table-cell>	
	    					</fo:table-row>
	    				</fo:table-header>
						<fo:table-body>
							<!-- maximum pulse -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Maximum pulse:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="maxPulse" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelHeartBeat" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- average pulse -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Average pulse:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="avgPulse" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelHeartBeat" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- average cadence -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Average cadence:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="avgCadence" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-body>
					</fo:table>
				</fo:block>
				
				
				<fo:block padding="0pt" margin-bottom="5pt" font-size="10pt">
					<fo:table border-style="solid" border-width="0.5pt" border-color="black" table-layout="fixed" width="100%">
						<fo:table-column column-width="4cm" />
						<fo:table-column column-width="8cm" />
						<fo:table-header>
	    					<fo:table-row>
	    						<fo:table-cell border-style="solid" border-width="0.5pt" border-color="black" text-align="center" vertical-align="middle" number-columns-spanned="2" background-color="#E1E1E1">
	    							<fo:block text-align="center" vertical-align="middle">
	    								Altitude
	    							</fo:block>
	    						</fo:table-cell>	
	    					</fo:table-row>
	    				</fo:table-header>
						<fo:table-body>
							<!-- highest altitude -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Highest altitude:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="maxAltitude" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelAltitude" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour meters up -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Meters up:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="tourAltUp" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelAltitude" /></fo:block>
								</fo:table-cell>
							</fo:table-row>
							<!-- tour meters down -->
							<fo:table-row>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="right" vertical-align="top">Meters down:</fo:block>
								</fo:table-cell>
								<fo:table-cell border-style="solid" border-width="0.5pt" padding="2pt">
									<fo:block text-align="left"><xsl:value-of select="tourAltDown" /><xsl:text>&#160;</xsl:text><xsl:value-of select="$unitLabelAltitude" /></fo:block>
								</fo:table-cell>
							</fo:table-row>	
						</fo:table-body>
					</fo:table>
				</fo:block>
				
				
				</fo:flow>				
			</fo:page-sequence>
		</fo:root>
	</xsl:template>
</xsl:stylesheet>