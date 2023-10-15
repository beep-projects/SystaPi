/*
* Copyright (c) 2021, The beep-projects contributors
* this file originated from https://github.com/beep-projects
* Do not remove the lines above.
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see https://www.gnu.org/licenses/
*
*/
package de.freaklamarsch.systarest;

/**
 * Class for holding the status of a Paradigma SystaComfort II heating
 * controller. The status is build from all the known fields from a SystaWeb UDP
 * data packet. If new fields are decoded, this class should be updated.
 */
public class SystaStatus {
	/**
	 * Aussentemperatur gemessen am Temperaturfühler TA, an der Außenwand des
	 * Gebäudes<br/>
	 * <br/>
	 * Outside temperature measured at the temperature sensor TA, on the outer wall
	 * of the building.
	 */
	public double outsideTemp;
	/**
	 * Vorlauftemperatur Heizkreis 1<br/>
	 * gemessen am Temperaturfühler TV, am Vorlauf von Heizkreis 1 (Rohrleitung, die
	 * zu den Heizkörpern hinführt).<br/>
	 * Anzeige erscheint nur bei Anlagen mit gemischtem Heizkreis.<br/>
	 * <br/>
	 * Flow temperature heating circuit 1<br/>
	 * measured at the temperature sensor TV, at the flow of heating circuit 1 (pipe
	 * leading to the radiators).<br/>
	 * Display appears only in systems with mixed heating circuit.
	 */
	public double circuit1FlowTemp;
	/**
	 * Rücklauftemperatur Heizkreis 1<br/>
	 * gemessen am Temperaturfühler TR, am Rücklauf von Heizkreis 1 (Rohrleitung,
	 * die von den Heizkörpern wegführt)<br/>
	 * <br/>
	 * Return temperature heating circuit 1<br/>
	 * Measured at temperature sensor TR, on the return of heating circuit 1 (pipe
	 * leading away from the radiators).
	 */
	public double circuit1ReturnTemp;
	/**
	 * Warmwassertemperatur TWO Temperatur im oberen Bereich des
	 * Trinkwasserspeichers oder Kombispeichers, gemessen am Temperaturfühler
	 * TWO<br/>
	 * <br/>
	 * Hot water temperature TWO Temperature in the upper area of the drinking water
	 * storage tank or combination storage tank, measured at the TWO temperature
	 * sensor.
	 */
	public double hotWaterTemp;
	/**
	 * Puffertemperatur oben TPO<br/>
	 * gemessen am Temperaturfühler TPO<br/>
	 * <ul>
	 * <li>Anlagen mit Pufferspeicher oder Kombispeicher: Temperatur im oberen
	 * Bereich des Pufferspeichers oder Kombispeichers</li>
	 * <li>Anlagen mit einstufigem Öl- oder Gaskessel: Temperatur im Kesselvorlauf
	 * des Heizkessels</li>
	 * </ul>
	 * Anzeige erscheint nur bei Anlagen mit Pufferspeichern oder Kombispeichern und
	 * bei Anlagen mit einstufigen Öl- oder Gaskesseln<br/>
	 * <br/>
	 * Buffer temperature top TPO<br/>
	 * measured at the temperature sensor TPO<br/>
	 * <ul>
	 * <li>Systems with buffer tank or combi tank: temperature in the upper part of
	 * the buffer tank or combi tank</li>
	 * <li>Systems with single-stage oil or gas boiler: temperature in the boiler
	 * flow of the boiler</li>
	 * </ul>
	 * Display appears only in systems with buffer storage tanks or combi-storage
	 * tanks and in systems with single-stage oil or gas boilers
	 */
	public double bufferTempTop;
	/**
	 * Puffertemperatur unten TPU<br/>
	 * gemessen am Temperaturfühler TPU, Temperatur im unteren Bereich des
	 * Pufferspeichers oder Kombispeichers. Anzeige erscheint nur bei Anlagen mit
	 * Pufferspeichern oder Kombispeichern.<br/>
	 * <br/>
	 * Buffer temperature bottom TPU<br/>
	 * Measured at the TPU temperature sensor, temperature in the lower area of the
	 * buffer storage tank or combi storage tank. Display appears only in systems
	 * with buffer storage tanks or combi storage tanks.
	 */
	public double bufferTempBottom;
	/**
	 * Zirkulationstemperatur<br/>
	 * gemessen am Temperaturfühler TZR am Rücklauf der Zirkulation. Anzeige
	 * erscheint nur bei Anlagen mit Zirkulationskreis und wenn ein Temperaturfühler
	 * TRZ angeschlossen ist.<br/>
	 * <br/>
	 * Circulation temperature<br/>
	 * measured at the temperature sensor TZR at the return of the circulation.
	 * Display appears only in systems with circulation circuit and when a
	 * temperature sensor TRZ is connected.
	 */
	public double circulationTemp;
	/**
	 * Vorlauf Heizkreis 2 TV<br/>
	 * Vorlauftemperatur Heizkreis 2 gemessen am Temperaturfühler TV2, am Vorlauf
	 * von Heizkreis 2 (Rohrleitung, die zu den Heizkörpern hinführt).<br/>
	 * Anzeige erscheint nur bei Anlagen mit 2 Heizkreisen<br/>
	 * <br/>
	 * Flow heating circuit 2 TV<br/>
	 * Flow temperature heating circuit 2 Measured at the temperature sensor TV2, at
	 * the flow of heating circuit 2 (pipe leading to the radiators)<br/>
	 * Display appears only in systems with 2 heating circuits
	 */
	public double circuit2FlowTemp;
	/**
	 * Rücklauf Heizkreis 2 TR<br/>
	 * Rücklauftemperatur Heizkreis 2 gemessen am Temperaturfühler TR2, am Rücklauf
	 * von Heizkreis 2 (Rohrleitung, die von den Heizkörpern wegführt)<br/>
	 * Anzeige erscheint nur bei Anlagen mit 2 Heizkreisen.<br/>
	 * <br/>
	 * Return flow heating circuit 2 TR<br/>
	 * Return temperature of heating circuit 2 Measured at temperature sensor TR2,
	 * at return of heating circuit 2 (pipe leading away from radiators).<br/>
	 * Display appears only in systems with 2 heating circuits.
	 */
	public double circuit2ReturnTemp;
	/**
	 * Raumtemperatur HK1 ist<br/>
	 * gemessen am Bedienteil mit eingebautem Temperaturfühler TO<br/>
	 * <br/>
	 * Room temperature HK1<br/>
	 * measured at the control panel with built-in temperature sensor TO
	 */
	public double roomTempActual1;
	/**
	 * Raumtemperatur HK2 ist<br/>
	 * gemessen am Bedienteil mit eingebautem Temperaturfühler TO<br/>
	 * <br/>
	 * Room temperature HK2<br/>
	 * measured at the control panel with built-in temperature sensor TO
	 */
	public double roomTempActual2;
	/**
	 * Kollektortemperatur ist<br/>
	 * Temperatur, gemessen am Temperaturfühler TSA im Kollektor<br/>
	 * <br/>
	 * Collector temperature<br/>
	 * Temperature measured at the temperature sensor TSA in the collector
	 */
	public double collectorTempActual;
	/**
	 * Vorlauf Kessel ist<br/>
	 * <br/>
	 * Flow boiler
	 */
	public double boilerFlowTemp;
	/**
	 * Rücklauf Kessel ist<br/>
	 * <br/>
	 * Return boiler
	 */
	public double boilerReturnTemp;
	/**
	 * Vorlauf Ofen ist<br/>
	 * <br/>
	 * Flow log boiler
	 */
	public double logBoilerFlowTemp;
	/**
	 * Rücklauf Ofen ist<br/>
	 * <br/>
	 * Return log boiler
	 */
	public double logBoilerReturnTemp;
	/**
	 * Holzkessel Puffer oben<br/>
	 * <br/>
	 * Log boiler buffer top
	 */
	public double logBoilerBufferTempTop;
	/**
	 * Schwimmbadtemperatur<br/>
	 * <br/>
	 * Swimming pool temperature
	 */
	public double swimmingpoolTemp;
	/**
	 * Vorlauf Schwimmbad<br/>
	 * <br/>
	 * Flow swimmingpool
	 */
	public double swimmingpoolFlowTemp;
	/**
	 * Rücklauf Schwimmbad<br/>
	 * <br/>
	 * Return swimmingpool
	 */
	public double swimmingpoolReturnTemp;
	/**
	 * Sollwert Warmwassertemperatur<br/>
	 * Aktuell gültiger Sollwert für die Warmwassertemperatur im Trinkwasserspeicher
	 * oder im oberen Bereich des Kombispeichers<br/>
	 * <br/>
	 * Hot water temperature set point<br/>
	 * currently valid setpoint for the hot water temperature in the drinking water
	 * storage tank or in the upper section of the combi storage tank
	 */
	public double hotWaterTempSet;
	/**
	 * Sollwert Raumtemperatur<br/>
	 * Aktuell gültiger Sollwert für die Raumtemperatur in Heizkreis 1<br/>
	 * <br/>
	 * Room temperature setpoint<br/>
	 * currently valid setpoint for room temperature in heating circuit 1
	 */
	public double roomTempSet1;
	/**
	 * Vorlauf Heizkreis TV soll<br/>
	 * Sollwert Vorlauftemperatur Heizkreis 1<br/>
	 * aktuell gültiger Sollwert für die Vorlauftemperatur in Heizkreis 1<br/>
	 * <br/>
	 * Flow heating circuit TV setpoint<br/>
	 * Setpoint for flow temperature heating circuit 1<br/>
	 * Currently valid setpoint for flow temperature in heating circuit 1
	 */
	public double circuit1FlowTempSet;
	/**
	 * Vorlauf Heizkreis 2 TV soll<br/>
	 * Sollwert Vorlauftemperatur Heizkreis 2<br/>
	 * Aktuell gültiger Sollwert für die Vorlauftemperatur im Heizkreis 2 Anzeige
	 * erscheint nur bei Anlagen mit 2 Heizkreisen.<br/>
	 * <br/>
	 * Flow heating circuit 2 TV set<br/>
	 * Setpoint for flow temperature heating circuit 2<br/>
	 * Currently valid setpoint for the flow temperature in heating circuit 2.
	 * Display only appears in systems with 2 heating circuits.
	 */
	public double circuit2FlowTempSet;
	/**
	 * Raumtemperatur 2 ist<br/>
	 * Sollwert Raumtemperatur Heizkreis 2<br/>
	 * aktuell gültiger Sollwert für die Raumtemperatur in Heizkreis 2<br/>
	 * Anzeige erscheint nur bei Anlagen mit 2 Heizkreisen<br/>
	 * <br/>
	 * room temperature 2 is<br/>
	 * Setpoint for room temperature in heating circuit 2<br/>
	 * currently valid setpoint for room temperature in heating circuit 2<br/>
	 * Display only appears in systems with 2 heating circuits
	 */
	public double roomTempSet2;
	/**
	 * Sollwert Puffertemperatur<br/>
	 * aktuell gültiger Sollwert für die Puffertemperatur<br/>
	 * Anzeige erscheint nur bei Anlagen mit Pufferspeichern oder
	 * Kombispeichern.<br/>
	 * <br/>
	 * Setpoint buffer temperature<br/>
	 * Currently valid setpoint for the buffer temperature.<br/>
	 * Display appears only for systems with buffer storage tanks or combi storage
	 * tanks.
	 */
	public double bufferTempSet;
	/**
	 * Kessel soll<br/>
	 * <br/>
	 * Boiler set
	 */
	public double boilerTempSet;
	/**
	 * Betriebsart<br/>
	 * 0 = Auto Prog. 1<br/>
	 * 1 = Auto Prog. 2<br/>
	 * 2 = Auto Prog. 3<br/>
	 * 3 = Dauernd Normal<br/>
	 * 4 = Dauernd Komfort<br/>
	 * 5 = Dauernd Absenken<br/>
	 * 6 = Sommer<br/>
	 * 7 = Aus<br/>
	 * 8 = Party<br/>
	 * 14= Test oder Kaminfeger<br/>
	 * <ul>
	 * <li>Automatik 1, 2 oder 3 - Anlage läuft im Regelbetrieb, gemäß den
	 * Einstellungen im Zeitprogramm 1, 2 oder 3 Trinkwassererwärmung und
	 * Zirkulation laufen entsprechend den Einstellungen im jeweiligen
	 * Zeitprogramm.</li>
	 * <li>Dauernd Normal - Heizkreis wird auf „Sollwert Raumtemperatur Normal“
	 * geregelt, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm.</li>
	 * <li>Dauernd Komfort - Heizkreis wird auf „Sollwert Raumtemperatur Komfort“
	 * geregelt, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm.</li>
	 * <li>Dauernd Abgesenkt - Heizkreis wird auf „Sollwert Raumtemperatur
	 * Abgesenkt“ geregelt, Trinkwassererwärmung und Zirkulation laufen entsprechend
	 * den Einstellungen im jeweiligen Zeitprogramm.</li>
	 * <li>Sommer - Heizung ist ausgeschaltet, Trinkwassererwärmung und Zirkulation
	 * laufen entsprechend den Einstellungen im jeweiligen Zeitprogramm.</li>
	 * <li>Aus - Heizung, Trinkwassererwärmung und Zirkulation sind ausgeschaltet,
	 * Frostschutz ist sichergestellt</li>
	 * <li>Party - Unabhängig vom Heizzeitprogramm verwendet der Regler den
	 * „Sollwert Raumtemperatur Normal“. Unabhängig vom Warmwasserzeitprogramm
	 * verwendet der Regler den „Sollwert Warmwassertemperatur Normal“. Die
	 * Zirkulation ist freigegeben.</li>
	 * </ul>
	 * <br/>
	 * Operating mode<br/>
	 * 0 = Auto Prog. 1<br/>
	 * 1 = Auto Prog. 2<br/>
	 * 2 = Auto Prog. 3<br/>
	 * 3 = Continuous Normal<br/>
	 * 4 = Continuous Comfort<br/>
	 * 5 = Continuous Lowering<br/>
	 * 6 = Summer<br/>
	 * 7 = Off<br/>
	 * 8 = Party<br/>
	 * 14= Test or chimney sweep<br/>
	 * <ul>
	 * <li>Automatic 1, 2 or 3 - System runs in control mode, according to the
	 * settings in time program 1, 2 or 3 DHW heating and circulation run according
	 * to the settings in the respective time program.</li>
	 * <li>Continuous Normal - Heating circuit is controlled to "Setpoint room
	 * temperature normal", DHW heating and circulation run according to the
	 * settings in the respective time program.</li>
	 * <li>Continuous Comfort - Heating circuit is controlled to "Comfort room
	 * temperature setpoint", DHW heating and circulation run according to the
	 * settings in the respective time program.</li>
	 * <li>Continuous Lowering - Heating circuit is controlled to "Room temperature
	 * setpoint reduced", drinking water heating and circulation run according to
	 * the settings in the respective time program.</li>
	 * <li>Summer - Heating is switched off, drinking water heating and circulation
	 * run according to the settings in the respective time program.</li>
	 * <li>Off - Heating, drinking water heating and circulation are switched off,
	 * frost protection is ensured.</li>
	 * <li>Party - Independent of the heating time program, the controller uses the
	 * "Setpoint room temperature normal". Independent of the hot water time
	 * program, the controller uses the "Setpoint hot water temperature normal".
	 * Circulation is enabled.</li>
	 * </ul>
	 */
	public int operationMode;
	/**
	 * operationModes = { "Auto Prog. 1", "Auto Prog. 2", "Auto Prog. 3",
	 * "Continuous Normal", "Continuous Comfort", "Continuous Lowering", "Summer",
	 * "Off", "Party", "undef", "undef", "undef", "manual", "Test", "chimney",
	 * "undef" }
	 */
	public final String[] operationModes = { "Auto Prog. 1", "Auto Prog. 2", "Auto Prog. 3", "Continuous Normal",
			"Continuous Comfort", "Continuous Lowering", "Summer", "Off", "Party", "undef", "undef", "undef", "manual",
			"Test", "chimney", "undef" };
	/**
	 * Status Kessel<br/>
	 * Wert Bedeutung<br/>
	 * 0 der Kessel ist aus<br/>
	 * 1 der Kessel ist an (Handbetrieb, Kurzschluss TR, Kaminfeger)<br/>
	 * 2 der Kessel bereitet Warmwasser<br/>
	 * 3 der Kessel ist für den Heizkreis an<br/>
	 * 4 der Kessel ist durch den Holzkessel (SystaComfort Wood) gesperrt<br/>
	 * 5 der Kessel ist durch den Wodtke-Pelletsofen (SystaComfort Stove)
	 * gesperrt<br/>
	 * 6 der Kessel ist gesperrt, weil die Außentemperatur über der
	 * Heizgrenztemperatur liegt<br/>
	 * 7 Wärmepumpe befindet sich im Kühlbetrieb<br/>
	 * 8 der Gasbrennwert-Kombikessel bereitet Warmwasser<br/>
	 * 9 der Kessel deckt den Warmwasserbedarf des Slaves (SystaComfort II MS)<br/>
	 * 10 Wärmepumpe befindet sich für den Slave im Kühlbetrieb (SystaComfort II
	 * MS)<br/>
	 * 11 der Kessel deckt den Heizbedarf des Slaves (SystaComfort II MS)<br/>
	 * 12 der Kessel ist nur auf Grund der Mindestlaufzeit aktiv<br/>
	 * 13 Startverzögerung der Kessel-Kaskade ist aktiv<br/>
	 * <br/>
	 * Betriebsart<br/>
	 * 0 = aus<br/>
	 * 1 = Handbetrieb<br/>
	 * 2 = Warmwasserbereitung<br/>
	 * 3 = Heizkreis heizen<br/>
	 * 4 = gesperrt (Holzkessel)<br/>
	 * 5 = gesperrt (Pelletsofen)<br/>
	 * 6 = gesperrt (Aussentemperatur)<br/>
	 * 7 = Kühlbetrieb<br/>
	 * 8 = Warmwasserbereitung (Kombikessel)<br/>
	 * 9 = Warmwasserbereitung (Slave)<br/>
	 * 10 = Kühlbetrieb (Slave)<br/>
	 * 11 = Heizkreis heizen (Slave)<br/>
	 * 12 = Mindestlaufzeit<br/>
	 * 13 = Startverzögerung aktiv<br/>
	 * 14= Test oder Kaminfeger<br/>
	 * <br/>
	 * Status Boiler<br/>
	 * Value Meaning<br/>
	 * 0 the boiler is off<br/>
	 * 1 boiler is on (manual operation, short circuit TR, chimney sweep)<br/>
	 * 2 boiler is preparing hot water<br/>
	 * 3 the boiler is on for the heating circuit<br/>
	 * 4 the boiler is blocked by the log boiler (SystaComfort Wood)<br/>
	 * 5 the boiler is blocked by the Wodtke pellet stove (SystaComfort Stove)<br/>
	 * 6 the boiler is blocked because the outdoor temperature is above the heating
	 * limit temperature<br/>
	 * 7 heat pump is in cooling mode<br/>
	 * 8 the gas condensing combi boiler is preparing hot water<br/>
	 * 9 the boiler covers the hot water demand of the slave (SystaComfort II
	 * MS)<br/>
	 * 10 heat pump is in cooling mode for the slave (SystaComfort II MS)<br/>
	 * 11 the boiler covers the heating demand of the slave (SystaComfort II
	 * MS)<br/>
	 * 12 the boiler is only active due to minimum running time<br/>
	 * 13 start delay of boiler cascade is active<br/>
	 * <br/>
	 * Operating mode<br/>
	 * 0 = off<br/>
	 * 1 = manual mode<br/>
	 * 2 = hot water preparation<br/>
	 * 3 = heating circuit<br/>
	 * 4 = blocked (log boiler)<br/>
	 * 5 = blocked (pellet stove)<br/>
	 * 6 = blocked (outside temperature)<br/>
	 * 7 = cooling mode<br/>
	 * 8 = hot water preparation (combi boiler)<br/>
	 * 9 = hot water preparation (slave)<br/>
	 * 10 = cooling mode (slave)<br/>
	 * 11 = heating circuit (slave)<br/>
	 * 12 = minimum running time<br/>
	 * 13 = start delay active
	 * 
	 * TODO clarify why SystaWeb uses "Aus","Ein","Ein Warmwasser","Ein
	 * Heizung","Gesperrt Holzkessel","Gesperrt Ofen","Gesperrt TA","Kühlbetrieb"]
	 */
	public int boilerOperationMode;
	/**
	 * boilerOperationModes = { "off", "manual", "hot water", "heating circuit",
	 * "blocked (log boiler)", "blocked (pellet stove)", "blocked (outside
	 * temperature)", "cooling mode", "hot water (combi boiler)", "hot water
	 * (slave)", "cooling mode (slave)", "heating circuit (slave)", "minimum running
	 * time", "start delay active" };
	 */
	public final String[] boilerOperationModeNames = { "off", "manual", "hot water", "heating circuit",
			"blocked (log boiler)", "blocked (pellet stove)", "blocked (outside temperature)", "cooling mode",
			"hot water (combi boiler)", "hot water (slave)", "cooling mode (slave)", "heating circuit (slave)",
			"minimum running time", "start delay active" };
	/**
	 * Raumtemperatur normal (soll)<br/>
	 * <br/>
	 * Room temperature normal (set)
	 */
	public double roomTempSetNormal;
	/**
	 * Raumtemperatur komfort (soll)<br/>
	 * <br/>
	 * Room temperature comfort (set)
	 */
	public double roomTempSetComfort;
	/**
	 * Raumtemperatur abgesenkt (soll)<br/>
	 * <br/>
	 * Room temperature lowering (set)
	 */
	public double roomTempSetLowering;
	/**
	 * Heizung aus=0, normal=1, komfort=2, absenken=3<br/>
	 * <br/>
	 * Heating off=0, normal=1, comfort=2, lower=3
	 */
	public int heatingOperationMode;
	/**
	 * heatingOperationModes = { "off", "normal", "comfort", "lowering" }
	 */
	public final String[] heatingOperationModes = { "off", "normal", "comfort", "lowering" };
	/**
	 * Regelung HK nach:<br/>
	 * 0 = Aussentemperatur<br/>
	 * 1 = Raumtemperatur<br/>
	 * 2 = TA/TI kombiniert<br/>
	 * 0 = außentemperaturgeführt<br/>
	 * 1 = raumtemperaturgeführt<br/>
	 * 2 = kombiniert:<br/>
	 * - tagsüber außentemperaturgeführt<br/>
	 * - nachts raumtemperaturgeführt<br/>
	 * <br/>
	 * Um die Raumtemperatur richtig zu messen, muss für jeden
	 * raumtemperaturgeführten Heizkreis ein Bedienteil im Wohnraum montiert
	 * sein.<br/>
	 * <br/>
	 * Control HK according to:<br/>
	 * 0 = outdoor temperature<br/>
	 * 1 = room temperature<br/>
	 * 2 = TA/TI combined<br/>
	 * 0 = outdoor temperature controlled<br/>
	 * 1 = room temperature controlled<br/>
	 * 2 = combined:<br/>
	 * - outdoor temperature controlled during the day<br/>
	 * - at night room temperature controlled<br/>
	 * <br/>
	 * In order to measure the room temperature correctly, a control panel must be
	 * installed in the living room for each room temperature controlled heating
	 * circuit.
	 */
	public int controlledBy;
	/**
	 * controlMethods = { "external temp", "room temp", "ext./room temp combined" }
	 */
	public final String[] controlMethods = { "external temp", "room temp", "ext./room temp combined" };
	/**
	 * Fusspunkt<br/>
	 * <br/>
	 * Base point
	 */
	public double heatingCurveBasePoint;
	/**
	 * Steilheit<br/>
	 * <br/>
	 * Gradient
	 */
	public double heatingCurveGradient;
	/**
	 * Maximale Vorlauf Temperatur<br/>
	 * <br/>
	 * Maximum flow temperature
	 */
	public double maxFlowTemp;
	/**
	 * Heizgrenze Heizbetrieb<br/>
	 * <br/>
	 * Heating limit heating mode
	 */
	public double heatingLimitTemp;
	/**
	 * Heizgrenze Absenken<br/>
	 * <br/>
	 * Heating limit Lowering
	 */
	public double heatingLimitTeampLowering;
	/**
	 * Frostschutz Aussentemperatur<br/>
	 * <br/>
	 * Anti freeze outside temperature
	 */
	public double antiFreezeOutsideTemp;
	/**
	 * Vorhaltezeit Aufheizen minuten<br/>
	 * <br/>
	 * Lead time heating up minutes
	 */
	public int heatUpTime; // in minutes
	/**
	 * Raumeinfluss<br/>
	 * <br/>
	 * Room impact
	 */
	public double roomImpact;
	/**
	 * Überhöhung Kessel<br/>
	 * <br/>
	 * Boiler superelevation
	 */
	public int boilerSuperelevation;
	/**
	 * Spreizung Heizkreis<br/>
	 * <br/>
	 * Spreading heating circuit
	 */
	public double heatingCircuitSpreading;
	/**
	 * Minimale Drehzahl Pumpe PHK %<br/>
	 * <br/>
	 * Minimum speed pump PHK %
	 */
	public int heatingPumpSpeedMin; // in %
	/**
	 * Drehzahl Pumpe PHK %<br/>
	 * <br/>
	 * Actual speed pump PHK %
	 */
	public int heatingPumpSpeedActual; // in %
	/**
	 * Mischer Laufzeit (minuten)<br/>
	 * <br/>
	 * Mixer runtime (minutes)
	 */
	public int mixerRuntime; // in minutes
	/**
	 * Raumtemperatur Abgleich (* 10, neg. Werte sind um 1 zu hoch, 0 und -1 werden
	 * beide als 0 geliefert)<br/>
	 * <br/>
	 * room temperature adjustment (* 10, neg. values are too high by 1, 0 and -1
	 * are both supplied as 0)
	 */
	public double roomTempCorrection;
	/**
	 * Fusspunkt Fussbodenheizung <br/>
	 * <br/>
	 * Base point underfloor heating
	 */
	public double underfloorHeatingBasePoint;
	/**
	 * Steilheit Fussbodenheitzung<br/>
	 * <br/>
	 * Gradient underfloor heating
	 */
	public double underfloorHeatingGradient;
	/**
	 * Warmwassertemperatur normal<br/>
	 * <br/>
	 * Hot water temperature normal
	 */
	public double hotWaterTempNormal;
	/**
	 * Warmwassertemperatur komfort<br/>
	 * <br/>
	 * Hot water temperature comfort
	 */
	public double hotWaterTempComfort;
	/**
	 * Warmwasser<br/>
	 * Aus=0<br/>
	 * Normal=1<br/>
	 * Komfort=2<br/>
	 * Gesperrt=3<br/>
	 * <br/>
	 * Hot water<br/>
	 * Off=0<br/>
	 * Normal=1<br/>
	 * Comfort=2<br/>
	 * Locked=3
	 */
	public int hotWaterOperationMode;
	/**
	 * hotWaterOperationModes = { "off", "normal", "comfort", "locked" }
	 */
	public final String[] hotWaterOperationModes = { "off", "normal", "comfort", "locked" };
	/**
	 * Schaltdifferenz Warmwasser<br/>
	 * <br/>
	 * Hysteresis hot water
	 */
	public double hotWaterHysteresis;
	/**
	 * Maximale Warmwassertemperatur<br/>
	 * <br/>
	 * Maximum hot water temperature
	 */
	public double hotWaterTempMax;
	/**
	 * Nachlauf Pumpe PK/LP<br/>
	 * <br/>
	 * Overrun pump PK/LP
	 */
	public int heatingPumpOverrun;
	/**
	 * Maximale Puffer Temperatur<br/>
	 * <br/>
	 * Maximum buffer temperature
	 */
	public double bufferTempMax;
	/**
	 * Minimale Puffer Temperatur<br/>
	 * <br/>
	 * Minimum buffer temperature
	 */
	public double bufferTempMin;
	/**
	 * 0 = "OPTIMA/EXPRESSO" 1 = "TITAN" 2 = "Puffer und ULV" 3 = "Puffer + LP" 4 =
	 * "Expressino" 5 = "Puffer u. Frischwasserstation"
	 * 
	 * 0 = "OPTIMA/EXPRESSO" 1 = "TITAN" 2 = "Buffer with DV" 3 = "Buffer + LP" 4 =
	 * "Expressino" 5 = "Buffer with fresh water statio"
	 */
	public int bufferType;
	public final String[] bufferTypeNames = { "OPTIMA/EXPRESSO", "TITAN", "Buffer with DV", "Buffer + LP", "Expressino",
			"Buffer with fresh water station" };
	/**
	 * SchaltDifferenz Kessel<br/>
	 * <br/>
	 * Hysteresis boiler
	 */
	public double boilerHysteresis;
	/**
	 * Minimale Laufzeit Kessel (minuten)<br/>
	 * <br/>
	 * Minimum boiler running time (minutes)
	 */
	public int boilerOperationTime;
	/**
	 * Abschalt TA Kessel<br/>
	 * <br/>
	 * Shutdown TA boiler
	 */
	public double boilerShutdownTemp;
	/**
	 * Minimale Drehzahl Pumpe PK %<br/>
	 * <br/>
	 * Minimum speed pump PK %
	 */
	public int boilerPumpSpeedMin;
	/**
	 * Kesselpumpe in %<br/>
	 * <br/>
	 * Boiler pump in %
	 */
	public int boilerPumpSpeedActual;
	/**
	 * Nachlaufzeit Pumpe PZ<br/>
	 * <br/>
	 * Overrun pump PZ
	 */
	public int circulationPumpOverrun;
	/**
	 * Zirkulation Schaltdifferenz<br/>
	 * <br/>
	 * Circulation hysteresis
	 */
	public double circulationHysteresis;
	/**
	 * Zirkulation Sperrzeit Taster<br/>
	 * <br/>
	 * Circulation lockout time push button
	 */
	public int circulationLockoutTimePushButton;
	/**
	 * Raumtemperatur ändern um<br/>
	 * <br/>
	 * Adjust room temperature by
	 */
	public double adjustRoomTempBy;
	/**
	 * Betriebszeit Kessel (Stunden)<br/>
	 * <br/>
	 * Boiler operating time (hours)
	 */
	public int boilerOperationTimeHours;
	/**
	 * Betriebszeit Kessel (Minuten)<br/>
	 * <br/>
	 * Boiler operating time (minutes)
	 */
	public int boilerOperationTimeMinutes;
	/**
	 * Anzahl Brennerstarts<br/>
	 * <br/>
	 * Number of burner starts
	 */
	public int burnerNumberOfStarts;
	/**
	 * Solare Leistung<br/>
	 * momentane Leistung der Solaranlage. Die solare Leistung berechnet sich aus
	 * folgenden Messwerten:<br/>
	 * <ul>
	 * <li>Differenz zwischen der Temperatur am Kollektoraustritt und der Temperatur
	 * am Kollektoreintritt</li>
	 * <li>Volumenstrom durch der Solaranlage</li>
	 * </ul>
	 * Solar power<br/>
	 * Instantaneous power of the solar system. The solar power is calculated from
	 * the following measured values:<br/>
	 * <ul>
	 * <li>difference between the temperature at the collector outlet and the
	 * temperature at the collector inlet</li>
	 * <li>volume flow through the solar system</li>
	 * </ul>
	 */
	public double solarPowerActual;
	/**
	 * Tagesgewinn<br/>
	 * die an diesem Tag bisher von der Solaranlage erzeugte Energiemenge. Die
	 * Anzeige wird um Mitternacht selbsttätig auf 0 zurückgesetzt.<br/>
	 * <br/>
	 * daily profit<br/>
	 * The amount of energy generated by the solar system on this day so far. The
	 * display is automatically reset to 0 at midnight.
	 */
	public double solarGainDay;
	/**
	 * Solargewinn gesamt<br/>
	 * die insgesamt von der Solaranlage erzeugte Energiemenge seit Inbetriebnahme
	 * der Solaranlage oder seit dem letzten Löschen des Solargewinns<br/>
	 * <br/>
	 * total solar gain<br/>
	 * the total amount of energy generated by the solar system since commissioning
	 * of the solar system or since the last deletion of the solar gain.
	 */
	public double solarGainTotal;
	public int systemNumberOfStarts;
	public int circuit1LeadTime;
	public int circuit2LeadTime;
	public int circuit3LeadTime;
	/**
	 * Relais<br/>
	 * Heizkreispumpe = Relais &amp; 0x0001<br/>
	 * Relais &amp; 0x0002<br/>
	 * Brenner = Relais &amp; 0x0004<br/>
	 * Mischer1 warm = Relais &amp; 0x0008<br/>
	 * Mischer1 kalt = Relais &amp; 0x0010<br/>
	 * Ladepumpe = Relais &amp; 0x0080<br/>
	 * Zirkulationspumpe = Relais &amp; 0x0100<br/>
	 * Kessel = Relais &amp; 0x0200<br/>
	 * Relais &amp; 0x0800<br/>
	 * Brenner = Kessel &amp;&amp; (FLOW_TEMP_BOILER - RETURN_TEMP_BOILER > 2) ||
	 * ((Relais &amp; 0x0004) != 0)<br/>
	 * Umlenkventil/Ladepumpe Holzkessel = Relais &amp; 0x1000<br/>
	 * LED Boiler = Relais &amp; 0x2000<br/>
	 * <br/>
	 * Relay<br/>
	 * Heating circuit pump = relay &amp; 0x0001<br/>
	 * Relay &amp; 0x0002<br/>
	 * Burner = Relay &amp; 0x0004<br/>
	 * Mixer warm = Relay &amp; 0x0008<br/>
	 * Mixer cool = Relay &amp; 0x0010<br/>
	 * Charge pump = Relay &amp; 0x0080<br/>
	 * Circulation Pump = Relay &amp; 0x0100<br/>
	 * Boiler = Relay &amp; 0x0200<br/>
	 * Relay &amp; 0x0800<br/>
	 * Burner = Relay &amp; 0x0004<br/>
	 * Charge pump log boiler = Relay &amp; 0x1000<br/>
	 * LED Boiler = Relay &amp; 0x2000
	 */
	public int relay;
	public static final int HEATING_PUMP_MASK = 0x0001;// bit 0
	public static final int UNKNOWN_1_MASK = 0x0002;// bit 1
	public static final int UNKNOWN_2_MASK = 0x0003;// bit 0 & 1
	public static final int BURNER_MASK = 0x0004;// bit 2
	public static final int MIXER_WARM_MASK = 0x0008;// bit 3
	public static final int MIXER_COLD_MASK = 0x0010;// bit 4
	public static final int CHARGE_PUMP_MASK = 0x0080;// bit 7
	public static final int CIRCULATION_PUMP_MASK = 0x0100;// bit 8
	public static final int BOILER_MASK = 0x0200; // bit 9
	public static final int UNKNOWN_5_MASK = 0x0800; // bit 11
	public static final int CHARGE_PUMP_LOG_BOILER_MASK = 0x1000; // bit 12 TODO verify this assumption
	public static final int LED_BOILER_MASK = 0x2000; // bit 13 TODO verify this assumption

	/**
	 * Heating circuit pump = relay &amp; 0x0001
	 */
	public boolean heatingPumpIsOn;
	/**
	 * Charge pump = Relay &amp; 0x0080
	 */
	public boolean chargePumpIsOn;
	/**
	 * log bioler charge pump = Relay &amp; 0x1000
	 */
	public boolean logBoilderChargePumpIsOn;
	/**
	 * LED boiler = Relay &amp; 0x2000
	 * 
	 * Ladezustand des Speichers Ein - Der Speicher ist voll, kein Holz mehr
	 * nachlegen. Aus - Der Speicher kann Wärme aufnehmen.
	 */
	public boolean boilerLedIsOn;
	/**
	 * Circulation Pump = Relay &amp; 0x0100
	 */
	public boolean circulationPumpIsOn;
	/**
	 * Boiler = Relay &amp; 0x0200
	 */
	public boolean boilerIsOn;
	/**
	 * Burner = relay &amp; 0x0004
	 */
	public boolean burnerIsOn;
	/**
	 * Relay &amp; 0x0002
	 */
	public boolean unknowRelayState1IsOn;
	/**
	 * Relay &amp; 0x0004
	 */
	public boolean unknowRelayState2IsOn;
	/**
	 * Relay &amp; 0x0008
	 */
	public boolean mixer1IsOnWarm;
	/**
	 * Relay &amp; 0x0010
	 */
	public boolean mixer1IsOnCool;
	/**
	 * 0 = 0ff 1 = cool 2 = warm 3 = undefined
	 */
	public int mixer1State;
	/**
	 * mixerStateNames = { "off", "cool", "warm", "undef" }
	 */
	public final String[] mixerStateNames = { "off", "cool", "warm", "undef" };
	/**
	 * Relay &amp; 0x0800
	 */
	public boolean unknowRelayState5IsOn;

	/**
	 * Fehlerstatus (65535 = OK)<br/>
	 * <br/>
	 * Error status (65535 = OK)
	 */
	public int error;
	/**
	 * Betriebsart ???<br/>
	 * <br/>
	 * Operating mode ???
	 */
	public int operationModeX;
	/**
	 * Heizung aus=0; normal=1, komfort=2, abgesenkt=3 ???<br/>
	 * <br/>
	 * heating off=0; normal=1, comfort=2, lowering=3 ???
	 */
	public int heatingOperationModeX;
	/**
	 * Minimale Temperatur im oberen Bereich des Pufferspeichers<br/>
	 * Bei Heizen nur mit Scheitholzkessel? = Ja gilt folendes:<br/>
	 * Wenn die Temperatur am Temperaturfühler TPO bzw. PO KH die minimale
	 * Temperatur unterschreitet, sperrt der Heizungsregler die Heizkreise
	 * (Anfahrentlastung).<br/>
	 * <br/>
	 * Minimum temperature in the upper part of the storage tank<br/>
	 * When heating only with a log boiler? = Yes the following applies:<br/>
	 * If the temperature at the temperature sensor TPO or PO KH falls below the
	 * minimum temperature, the heating controller blocks the heating circuits
	 * (start-up). heating controller blocks the heating circuits (start-up
	 * unloading).
	 */
	public double logBoilerBufferTempMin;
	/**
	 * Minimale Temperatur des Scheitholzkessels<br/>
	 * Der Scheitholzkessel darf die minimale Kesseltemperatur im Betrieb nicht
	 * unterschreiten, um Kondensatbildung im Kessel zu vermeiden. Die Temperatur
	 * wird am Temperaturfühler TV KH gemessen.<br/>
	 * <br/>
	 * Minimum temperature of the log boiler<br/>
	 * The log boiler must not fall below the minimum boiler temperature during
	 * operation, in order to avoid condensate formation in the boiler. The
	 * temperature is measured at temperature sensor TV KH.
	 */
	public double logBoilerTempMin;
	/**
	 * Minimaler Sollwert für die Temperaturdifferenz zwischen Kesselvorlauf TVKH
	 * und Kesselrücklauf TRKH<br/>
	 * Der Heizungsregler vergleicht den Sollwert mit der tatsächlichen
	 * Temperaturdifferenz und schaltet die Pumpe des Scheitholzkessels ein oder
	 * aus. Wenn die Temperaturdifferenz 2 K kleiner ist als der Sollwert, schaltet
	 * die Pumpe ab. Wenn die Temperaturdifferenz größer ist als der Sollwert,
	 * schaltet die Pumpe ein.<br/>
	 * <br/>
	 * Minimum set point for the temperature difference between boiler flow TVKH and
	 * boiler return TRKH<br/>
	 * The heating controller compares the setpoint value with the actual
	 * temperature difference and switches the pump of the * log boiler on or off.
	 * log boiler on or off. If the temperature difference is 2 K less than the set
	 * point, the pump switches off. If the temperature difference is greater than
	 * the set point, the pump switches on.
	 */
	public double logBoilerSpreadingMin;
	/**
	 * Minimale Drehzahl der Kesselpumpe PKH.<br/>
	 * <br/>
	 * Minimum speed of the boiler pump PKH.
	 */
	public int logBoilerPumpSpeedMin;
	/**
	 * Holzkessel pumpe in %<br/>
	 * <br/>
	 * logBoiler pump in %
	 */
	public int logBoilerPumpSpeedActual;
	public int logBoilerSettings;
	public static final int LOG_BOILER_PARALLEL_OPERATION_MASK = 0x0008;
	public static final int BOILER_HEATS_BUFFER_MASK = 0x0010;
	/**
	 * Parallelbetrieb<br/>
	 * Nein - der Hauptkessel wird gesperrt, wenn der Scheitholzkessel in Betrieb
	 * ist<br/>
	 * Ja - der Hauptkessel wird nicht gesperrt, wenn der Scheitholzkessel in
	 * Betrieb ist<br/>
	 * <br/>
	 * Parallel operation<br/>
	 * No - the main boiler is blocked when the log boiler is in operation.<br/>
	 * Yes - the main boiler is not blocked when the log boiler is in operation.
	 */
	public boolean logBoilerParallelOperation;

	/**
	 * 0 = "Aus" 1 = "Anheizen" 2 = "Leistungsbrand" 3 = "Ausbrand" 4 = "Abschalten"
	 * 5 = "Nachkühlen" 6 = "Anschieben" Aus Der Scheitholzkessel liefert keine
	 * Wärme. - Pumpe des Scheitholzkessels ist ausgeschaltet - Hauptkessel ist
	 * freigegeben Wenn der Scheitholzkessel die minimale Kesseltemperatur erreicht
	 * hat, erfolgt der Übergang in den Status Anheizen. Anheizen Der
	 * Scheitholzkessel liefert noch nicht genügend Wärme. - Hauptkessel ist
	 * gesperrt, wenn - Hauptkessel heizt Pufferspeicher: Ja - Parallelbetrieb: Nein
	 * Wenn der Scheitholzkessel die minimale Kesseltemperatur plus Spreizung
	 * erreicht hat, erfolgt der Übergang in den Status Leistungsbrand.
	 * Leistungsbrand Der Scheitholzkessel liefert Wärme. - Pumpe des
	 * Scheitholzkessels (PKH) schaltet ein - Pumpe des Scheitholzkessels (PKH)
	 * schaltet aus, wenn die Spreizung zu klein ist Wenn der Scheitholzkessel die
	 * minimale Kesseltemperatur unterschreitet, erfolgt der Übergang in den Status
	 * Ausbrand. Ausbrand Der Scheitholzkessel liefert zu wenig Wärme. - Pumpe des
	 * Scheitholzkessels (PKH) schaltet aus Nach 15 Minuten erfolgt der Übergang in
	 * den Status Aus. Anschieben Die Pumpe des Scheitholzkessels (PKH) schaltet
	 * kurz ein, damit die Kesseltemperatur am Temperaturfühler (TVKH) korrekt
	 * gemessen werden kann. Nachkühlen Wenn der Scheitholzkessel in der Anheizphase
	 * die geforderte Temperatur über einen bestimmten Zeitraum nicht erreicht,
	 * schaltet die Pumpe des Scheitholzkessels (PKH) für 15 Minuten ein. Die Wärme
	 * wird aus dem Scheitholzkessel transportiert.
	 * 
	 * 0 = "off" 1 = "heat-up" 2 = "power firing" 3 = "burnout" 4 = "switch off" 5 =
	 * "post-cooling" 6 = "push" Off The log boiler does not supply heat. - Pump of
	 * the log boiler is switched off. - Main boiler is enabled When the log boiler
	 * has reached the minimum boiler temperature, the transition to the heat-up
	 * status takes place. Heat-up The log boiler does not provide enough heat yet.
	 * - Main boiler is blocked if - Main boiler is heating buffer: Yes - Parallel
	 * operation: No When the log boiler has reached the minimum boiler temperature
	 * plus spread, the transition to the power fire status occurs. Power firing The
	 * log boiler supplies heat. - Pump of the log boiler (PKH) switches on. - Pump
	 * of the log boiler (PKH) switches off when the spreading is too small. When
	 * the log boiler falls below the minimum boiler temperature, the transition to
	 * the burnout status takes place. Burnout The log boiler provides too little
	 * heat. - Pump of the log boiler (PKH) switches off After 15 minutes the
	 * transition to the Off status takes place. Push The pump of the log boiler
	 * (PKH) switches on briefly so that the boiler temperature can be measured
	 * correctly at the temperature sensor (TVKH). Post-cooling If the log boiler
	 * does not reach the required temperature for a certain period of time during
	 * the heating-up phase, the pump of the log boiler (PKH) switches on for 15
	 * minutes. The heat is transported from the log boiler.
	 */
	public int logBoilerOperationMode;
	/**
	 * logBoilerOperationModeNames = {"off","heat-up","power
	 * firing","burnout","switch off","post-cooling","push"}
	 */
	public final String[] logBoilerOperationModeNames = { "off", "heat-up", "power firing", "burnout", "switch off",
			"post-cooling", "push" };
	/**
	 * Einstellung, welche Anlagenvariante verwendet wird<br/>
	 * Nein - Anlagen mit Pufferspeicher, bei denen ausschließlich der
	 * Scheitholzkessel den Speicher erwärmt<br/>
	 * Ja - Anlagen mit Pufferspeicher/Kombispeicher, bei denen der Hauptkessel den
	 * Speicher erwärmt<br/>
	 * <br/>
	 * Setting which system variant is used<br/>
	 * No - systems with buffer tank, where only the log boiler heats the tank.<br/>
	 * Yes - systems with storage tank/combined storage tank, where the main boiler
	 * heats the storage tank.
	 */
	public boolean boilerHeatsBuffer;

	/**
	 * 0 = "Aus" 1 = "Aus Heizgrenze" 2 = "Aus TI" 3 = "Gesperrt TPO" 4 = "Aus
	 * WW-Vorrang" 5 = "Ein" 6 = "Frostschutz" 7 = "Kühlen" 8 = "Vorhaltezeit" 9 =
	 * "Heizbetrieb" 10 = "Komfortbetrieb" 11 = "Absenkbetrieb" 12 = "Aus TSB" 13 =
	 * "Gesperrt" 14 = "Normal" 15 = "Erhöht" 16 = "WW-Modus" 17 = "Estrich
	 * trocknen" 18 = "Kühlbetrieb"
	 * 
	 * 0 = "off" 1 = "off (heating limit)" 2 = "off (room temperature)" 3 = "locked
	 * (buffer temp top)" 4 = "off (yield hot water)" 5 = "on" 6 = "anti freeze" 7 =
	 * "cooling" 8 = "lead time heat up" 9 = "heating" 10 = "comfort" 11 =
	 * "lowering" 12 = "off (temp swimming pool)" 13 = "locked" 14 = "normal" 15 =
	 * "raised" 16 = "hot water" 17 = "screed heating" 18 = "cooling mode"
	 */
	public int circuit1OperationMode;
	public final String[] circuit1OperationModeNames = { "off", "off (heating limit)", "off (room temperature)",
			"locked (buffer temp top)", "off (yield hot water)", "on", "anti freeze", "cooling", "lead time heat up",
			"heating", "comfort", "lowering", "off (temp swimming pool)", "locked", "normal", "raised", "hot water",
			"screed heating", "cooling mode" };

	/**
	 * Status Zirkulation 0="Aus" 1="Nachlauf" 2="Sperrzeit" 3="Gesperrt" 4="Aus
	 * F\u00fchler TZR" 5="Ein" 6="Frost"
	 * 
	 * 0="off" 1="overrun" 2="locked (time)" 3="locked" 4="off (temperature)" 5="on"
	 * 6="anti freeze"
	 */
	public int circulationOperationMode;
	public final String[] circulationOperationModeNames = { "off", "overrun", "locked (time)", "locked",
			"off (temperature)", "on", "anti freeze" };

	/**
	 * timestamp for this status in number of milliseconds from the epoch of
	 * 1970-01-01T00:00:00Z (UTC)
	 */
	public long timestamp;
	/**
	 * ISO 8601 compliant textual representation of the timestamp for this status
	 */
	public String timestampString;

}
