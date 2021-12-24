/*
 * Copyright (c) 2021, The beep-projects contributors
 * this file originated from https://github.com/beep-projects
 * Do not remove the lines above.
 * The rest of this source code is subject to the terms of the Mozilla Public License.
 * You can obtain a copy of the MPL at <https://www.mozilla.org/MPL/2.0/>.
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
	 * Gebäudes
	 *
	 * Outside temperature measured at the temperature sensor TA, on the outer wall
	 * of the building.
	 */
	public double outsideTemp;
	/**
	 * Vorlauftemperatur Heizkreis 1 gemessen am Temperaturfühler TV, am Vorlauf von
	 * Heizkreis 1 (Rohrleitung, die zu den Heizkörpern hinführt) Anzeige erscheint
	 * nur bei Anlagen mit gemischtem Heizkreis.
	 *
	 * Flow temperature heating circuit 1 measured at the temperature sensor TV, at
	 * the flow of heating circuit 1 (pipe leading to the radiators). Display
	 * appears only in systems with mixed heating circuit.
	 */
	public double circuit1FlowTemp;
	/**
	 * Rücklauftemperatur Heizkreis 1 gemessen am Temperaturfühler TR, am Rücklauf
	 * von Heizkreis 1 (Rohrleitung, die von den Heizkörpern wegführt)
	 *
	 * Return temperature heating circuit 1 Measured at temperature sensor TR, on
	 * the return of heating circuit 1 (pipe leading away from the radiators).
	 */
	public double circuit1ReturnTemp;
	/**
	 * Warmwassertemperatur TWO Temperatur im oberen Bereich des
	 * Trinkwasserspeichers oder Kombispeichers, gemessen am Temperaturfühler TWO
	 *
	 * Hot water temperature TWO Temperature in the upper area of the drinking water
	 * storage tank or combination storage tank, measured at the TWO temperature
	 * sensor.
	 */
	public double hotWaterTemp;
	/**
	 * Puffertemperatur oben TPO gemessen am Temperaturfühler TPO Anlagen mit
	 * Pufferspeicher oder Kombispeicher: Temperatur im oberen Bereich des
	 * Pufferspeichers oder Kombispeichers Anlagen mit einstufigem Öl- oder
	 * Gaskessel: Temperatur im Kesselvorlauf des Heizkessels Anzeige erscheint nur
	 * bei Anlagen mit Pufferspeichern oder Kombispeichern und bei Anlagen mit
	 * einstufigen Öl- oder Gaskesseln
	 *
	 * Buffer temperature top TPO measured at the temperature sensor TPO Systems
	 * with buffer tank or combi tank: temperature in the upper part of the buffer
	 * tank or combi tank Systems with single-stage oil or gas boiler: temperature
	 * in the boiler flow of the boiler Display appears only in systems with buffer
	 * storage tanks or combi-storage tanks and in systems with single-stage oil or
	 * gas boilers
	 */
	public double bufferTempTop;
	/**
	 * Puffertemperatur unten TPU gemessen am Temperaturfühler TPU, Temperatur im
	 * unteren Bereich des Pufferspeichers oder Kombispeichers Anzeige erscheint nur
	 * bei Anlagen mit Pufferspeichern oder Kombispeichern.
	 *
	 * Buffer temperature bottom TPU Measured at the TPU temperature sensor,
	 * temperature in the lower area of the buffer storage tank or combi storage
	 * tank. Display appears only in systems with buffer storage tanks or combi
	 * storage tanks.
	 */
	public double bufferTempBottom;
	/**
	 * (Zirkulationstemperatur) gemessen am Temperaturfühler TZR am Rücklauf der
	 * Zirkulation Anzeige erscheint nur bei Anlagen mit Zirkulationskreis und wenn
	 * ein Temperaturfühler TRZ angeschlossen ist.
	 *
	 * (circulation temperature) measured at the temperature sensor TZR at the
	 * return of the circulation. Display appears only in systems with circulation
	 * circuit and when a temperature sensor TRZ is connected.
	 */
	public double circulationTemp;
	/**
	 * (Vorlauf Heizkreis 2 TV) Vorlauftemperatur Heizkreis 2 gemessen am
	 * Temperaturfühler TV2, am Vorlauf von Heizkreis 2 (Rohrleitung, die zu den
	 * Heizkörpern hinführt) Anzeige erscheint nur bei Anlagen mit 2 Heizkreisen
	 *
	 * (Flow heating circuit 2 TV) Flow temperature heating circuit 2 Measured at
	 * the temperature sensor TV2, at the flow of heating circuit 2 (pipe leading to
	 * the radiators). Display appears only in systems with 2 heating circuits
	 */
	public double circuit2FlowTemp;
	/**
	 * (Rücklauf Heizkreis 2 TR) Rücklauftemperatur Heizkreis 2 gemessen am
	 * Temperaturfühler TR2, am Rücklauf von Heizkreis 2 (Rohrleitung, die von den
	 * Heizkörpern wegführt) Anzeige erscheint nur bei Anlagen mit 2 Heizkreisen.
	 *
	 * (Return flow heating circuit 2 TR) Return temperature of heating circuit 2
	 * Measured at temperature sensor TR2, at return of heating circuit 2 (pipe
	 * leading away from radiators). Display appears only in systems with 2 heating
	 * circuits.
	 */
	public double circuit2ReturnTemp;
	/**
	 * Raumtemperatur HK1 ist gemessen am Bedienteil mit eingebautem
	 * Temperaturfühler TO
	 *
	 * Room temperature HK1 measured at the control panel with built-in temperature
	 * sensor TO
	 */
	public double roomTempActual1;
	/**
	 * Raumtemperatur HK2 ist gemessen am Bedienteil mit eingebautem
	 * Temperaturfühler TO
	 *
	 * Room temperature HK2 measured at the control panel with built-in temperature
	 * sensor TO
	 */
	public double roomTempActual2;
	/**
	 * Kollektortemperatur ist Temperatur, gemessen am Temperaturfühler TSA im
	 * Kollektor
	 *
	 * Collector temperature Temperature measured at the temperature sensor TSA in
	 * the collector
	 */
	public double collectorTempActual;
	/**
	 * Vorlauf Kessel ist
	 *
	 * Flow boiler
	 */
	public double boilerFlowTemp;
	/**
	 * Rücklauf Kessel ist
	 *
	 * Return boiler
	 */
	public double boilerReturnTemp;
	/**
	 * Vorlauf Ofen ist
	 *
	 * Flow log boiler
	 */
	public double logBoilerFlowTemp;
	/**
	 * Rücklauf Ofen ist
	 *
	 * Return logBoiler
	 */
	public double logBoilerReturnTemp;
	/**
	 * (Holzkessel Puffer oben)
	 *
	 * (log boiler buffer top)
	 */
	public double logBoilerBufferTempTop;
	/**
	 * (Schwimmbadtemperatur)
	 *
	 * (swimming pool temperature)
	 */
	public double swimmingpoolFlowTemp;
	/**
	 * (Vorlauf Schwimmbad)
	 *
	 * (Flow swimmingpool)
	 */
	public double swimmingpoolFlowTeamp;
	/**
	 * (Rücklauf Schwimmbad)
	 *
	 * (Return swimmingpool)
	 */
	public double swimmingpoolReturnTemp;
	/**
	 * Sollwert Warmwassertemperatur aktuell gültiger Sollwert für die
	 * Warmwassertemperatur im Trinkwasserspeicher oder im oberen Bereich des
	 * Kombispeichers
	 *
	 * Hot water temperature set point currently valid setpoint for the hot water
	 * temperature in the drinking water storage tank or in the upper section of the
	 * combi storage tank
	 */
	public double hotWaterTempSet;
	/**
	 * Sollwert Raumtemperatur aktuell gültiger Sollwert für die Raumtemperatur in
	 * Heizkreis 1
	 *
	 * Room temperature setpoint currently valid setpoint for room temperature in
	 * heating circuit 1
	 */
	public double roomTempSet1;
	/**
	 * Vorlauf Heizkreis TV soll Sollwert Vorlauftemperatur Heizkreis 1 aktuell
	 * gültiger Sollwert für die Vorlauftemperatur in Heizkreis 1
	 *
	 * Flow heating circuit TV to be Setpoint for flow temperature heating circuit 1
	 * Currently valid setpoint for flow temperature in heating circuit 1
	 */
	public double circuit1FlowTempSet;
	/**
	 * (Vorlauf Heizkreis 2 TV soll) Sollwert Vorlauftemperatur Heizkreis 2 aktuell
	 * gültiger Sollwert für die Vorlauftemperatur im Heizkreis 2 Anzeige erscheint
	 * nur bei Anlagen mit 2 Heizkreisen.
	 *
	 * (Flow heating circuit 2 TV set) Setpoint for flow temperature heating circuit
	 * 2 Currently valid setpoint for the flow temperature in heating circuit 2.
	 * Display only appears in systems with 2 heating circuits.
	 */
	public double circuit2FlowTempSet;
	/**
	 * (Raumtemperatur 2 ist) Sollwert Raumtemperatur Heizkreis 2 aktuell gültiger
	 * Sollwert für die Raumtemperatur in Heizkreis 2 Anzeige erscheint nur bei
	 * Anlagen mit 2 Heizkreisen
	 *
	 * (room temperature 2 is) Setpoint for room temperature in heating circuit 2
	 * currently valid setpoint for room temperature in heating circuit 2 Display
	 * only appears in systems with 2 heating circuits
	 */
	public double roomTempSet2;
	/**
	 * Sollwert Puffertemperatur aktuell gültiger Sollwert für die Puffertemperatur
	 * Anzeige erscheint nur bei Anlagen mit Pufferspeichern oder Kombispeichern.
	 *
	 * Setpoint buffer temperature Currently valid setpoint for the buffer
	 * temperature. Display appears only for systems with buffer storage tanks or
	 * combi storage tanks.
	 */
	public double bufferTempSet;
	/**
	 * Kessel soll
	 *
	 * Boiler set
	 */
	public double boilerTempSet;
	/**
	 * Betriebsart
	 * 0 = Auto Prog. 1
	 * 1 = Auto Prog. 2
	 * 2 = Auto Prog. 3
	 * 3 = Dauernd Normal
	 * 4 = Dauernd Komfort
	 * 5 = Dauernd Absenken
	 * 6 = Sommer
	 * 7 = Aus
	 * 8 = Party
	 * 14= Test oder Kaminfeger
	 *
	 * • Automatik 1, 2 oder 3 - Anlage läuft im Regelbetrieb, gemäß den
	 * Einstellungen im Zeitprogramm 1, 2 oder 3 Trinkwassererwärmung und
	 * Zirkulation laufen entsprechend den Einstellungen im jeweiligen Zeitprogramm.
	 *
	 * • Dauernd Normal - Heizkreis wird auf „Sollwert Raumtemperatur Normal“
	 * geregelt, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm.
	 *
	 * • Dauernd Komfort - Heizkreis wird auf „Sollwert Raumtemperatur Komfort“
	 * geregelt, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm.
	 *
	 * • Dauernd Abgesenkt - Heizkreis wird auf „Sollwert Raumtemperatur Abgesenkt“
	 * geregelt, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm.
	 *
	 * • Sommer - Heizung ist ausgeschaltet, Trinkwassererwärmung und Zirkulation
	 * laufen entsprechend den Einstellungen im jeweiligen Zeitprogramm.
	 *
	 * • Aus - Heizung, Trinkwassererwärmung und Zirkulation sind ausgeschaltet,
	 * Frostschutz ist sichergestellt
	 *
	 * • Party - Unabhängig vom Heizzeitprogramm verwendet der Regler den „Sollwert
	 * Raumtemperatur Normal“. Unabhängig vom Warmwasserzeitprogramm verwendet der
	 * Regler den „Sollwert Warmwassertemperatur Normal“. Die Zirkulation ist
	 * freigegeben.
	 *
	 * Operating mode 
	 * 0 = Auto Prog. 1
	 * 1 = Auto Prog. 2
	 * 2 = Auto Prog. 3
	 * 3 = Continuous Normal
	 * 4 = Continuous Comfort
	 * 5 = Continuous Lowering
	 * 6 = Summer
	 * 7 = Off
	 * 8 = Party
	 * 14= Test or chimney sweep
	 *
	 * • Automatic 1, 2 or 3 - System runs in control mode, according to the
	 * settings in time program 1, 2 or 3 DHW heating and circulation run according
	 * to the settings in the respective time program. 
	 * 
	 * • Continuous Normal - Heating
	 * circuit is controlled to "Setpoint room temperature normal", DHW heating and
	 * circulation run according to the settings in the respective time program. 
	 * 
	 * • Continuous Comfort - Heating circuit is controlled to "Comfort room
	 * temperature setpoint", DHW heating and circulation run according to the
	 * settings in the respective time program. 
	 * 
	 * • Continuous Lowering - Heating circuit is controlled to "Room temperature setpoint reduced",
	 * drinking water heating and circulation run according to the settings in the respective time
	 * program.
	 * 
	 * • Summer - Heating is switched off, drinking water heating and
	 * circulation run according to the settings in the respective time program. 
	 * 
	 * • Off - Heating, drinking water heating and circulation are switched off, frost
	 * protection is ensured. 
	 * 
	 * • Party - Independent of the heating time program, the controller uses the "Setpoint room temperature normal".
	 * Independent of the hot water time program, the controller uses the "Setpoint hot water
	 * temperature normal". Circulation is enabled.
	 */
	public int operationMode;
	public final String[] operationModes = { "Auto Prog. 1", "Auto Prog. 2", "Auto Prog. 3", "Continuous Normal",
			"Continuous Comfort", "Continuous Lowering", "Summer", "Off", "Party", "undef", "undef", "undef", "manual",
			"Test", "chimney", "undef" };
	/**
	 * Status Kessel (Holding Registers, Adresse 41) Wert Bedeutung 0 der Kessel ist
	 * aus 1 der Kessel ist an (Handbetrieb, Kurzschluss TR, Kaminfeger) 2 der
	 * Kessel bereitet Warmwasser 3 der Kessel ist für den Heizkreis an 4 der Kessel
	 * ist durch den Holzkessel (SystaComfort Wood) gesperrt 5 der Kessel ist durch
	 * den Wodtke-Pelletsofen (SystaComfort Stove) gesperrt 6 der Kessel ist
	 * gesperrt, weil die Außentemperatur über der Heizgrenztemperatur liegt 7
	 * Wärmepumpe befindet sich im Kühlbetrieb 8 der Gasbrennwert-Kombikessel
	 * bereitet Warmwasser 9 der Kessel deckt den Warmwasserbedarf des Slaves
	 * (SystaComfort II MS) 10 Wärmepumpe befindet sich für den Slave im Kühlbetrieb
	 * (SystaComfort II MS) 11 der Kessel deckt den Heizbedarf des Slaves
	 * (SystaComfort II MS) 12 der Kessel ist nur auf Grund der Mindestlaufzeit
	 * aktiv 13 Startverzögerung der Kessel-Kaskade ist aktiv
	 *
	 * Betriebsart 0 = aus 1 = Handbetrieb 2 = Warmwasserbereitung 3 = Heizkreis
	 * heizen 4 = gesperrt (Holzkessel) 5 = gesperrt (Pelletsofen) 6 = gesperrt
	 * (Aussentemperatur) 7 = Kühlbetrieb 8 = Warmwasserbereitung (Kombikessel) 9 =
	 * Warmwasserbereitung (Slave) 10 = Kühlbetrieb (Slave) 11 = Heizkreis heizen
	 * (Slave) 12 = Mindestlaufzeit 13 = Startverzögerung aktiv 14= Test oder
	 * Kaminfeger
	 *
	 *
	 * Status Boiler (Holding Registers, Address 41) Value Meaning 0 the boiler is
	 * off 1 boiler is on (manual operation, short circuit TR, chimney sweep) 2
	 * boiler is preparing hot water 3 the boiler is on for the heating circuit 4
	 * the boiler is blocked by the log boiler (SystaComfort Wood) 5 the boiler is
	 * blocked by the Wodtke pellet stove (SystaComfort Stove) 6 the boiler is
	 * blocked because the outdoor temperature is above the heating limit
	 * temperature 7 heat pump is in cooling mode 8 the gas condensing combi boiler
	 * is preparing hot water 9 the boiler covers the hot water demand of the slave
	 * (SystaComfort II MS) 10 heat pump is in cooling mode for the slave
	 * (SystaComfort II MS) 11 the boiler covers the heating demand of the slave
	 * (SystaComfort II MS) 12 the boiler is only active due to minimum running time
	 * 13 start delay of boiler cascade is active
	 *
	 * Operating mode 0 = off 1 = manual mode 2 = hot water preparation 3 = heating
	 * circuit 4 = blocked (log boiler) 5 = blocked (pellet stove) 6 = blocked
	 * (outside temperature) 7 = cooling mode 8 = hot water preparation (combi
	 * boiler) 9 = hot water preparation (slave) 10 = cooling mode (slave) 11 =
	 * heating circuit (slave) 12 = minimum running time 13 = start delay active
	 */
	public int boilerOperationMode;
	public final String[] boilerOperationModes = { "off", "manual", "hot water", "heating circuit",
			"blocked (log boiler)", "blocked (pellet stove)", "blocked (outside temperature)", "cooling mode",
			"hot water (combi boiler)", "hot water (slave)", "cooling mode (slave)", "heating circuit (slave)",
			"minimum running time", "start delay active" };
	/**
	 * Raumtemperatur normal (soll)
	 *
	 * Room temperature normal (set)
	 */
	public double roomTempSetNormal;
	/**
	 * Raumtemperatur komfort (soll)
	 *
	 * Room temperature comfort (set)
	 */
	public double roomTempSetComfort;
	/**
	 * Raumtemperatur abgesenkt (soll)
	 *
	 * Room temperature lowering (set)
	 */
	public double roomTempSetLowering;
	/**
	 * Heizung aus=0, normal=1, komfort=2, absenken=3
	 *
	 * Heating off=0, normal=1, comfort=2, lower=3
	 */
	public int heatingOperationMode;
	public final String[] heatingOperationModes = { "off", "normal", "comfort", "lowering" };
	/**
	 * Regelung HK nach: 0=Aussentemperatur, 1=Raumtemperatur, 2= TA/TI kombiniert 0
	 * = außentemperaturgeführt 1 = raumtemperaturgeführt 2 = kombiniert: - tagsüber
	 * außentemperaturgeführt - nachts raumtemperaturgeführt
	 *
	 * Um die Raumtemperatur richtig zu messen, muss für jeden
	 * raumtemperaturgeführten Heizkreis ein Bedienteil im Wohnraum montiert sein.
	 *
	 * Control HK according to: 0=outdoor temperature, 1=room temperature, 2= TA/TI
	 * combined. 0 = outdoor temperature controlled 1 = room temperature controlled
	 * 2 = combined: - outdoor temperature controlled during the day - at night room
	 * temperature controlled
	 *
	 * In order to measure the room temperature correctly, a control panel must be
	 * installed in the living room for each room temperature controlled heating
	 * circuit.
	 */
	public int controlledBy;
	public final String[] controlMethods = { "external temp", "room temp", "ext./room temp combined" };
	/**
	 * Fusspunkt
	 *
	 * Base point
	 */
	public double heatingCurveBasePoint;
	/**
	 * Steilheit
	 *
	 * Gradient
	 */
	public double heatingCurveGradient;
	/**
	 * Maximale Vorlauf Temperatur
	 *
	 * Maximum flow temperature
	 */
	public double maxFlowTemp;
	/**
	 * Heizgrenze Heizbetrieb
	 *
	 * Heating limit heating mode
	 */
	public double heatingLimitTemp;
	/**
	 * Heizgrenze Absenken
	 *
	 * Heating limit Lowering
	 */
	public double heatingLimitTeampLowering;
	/**
	 * Frostschutz Aussentemperatur
	 *
	 * Anti freeze outside temperature
	 */
	public double antiFreezeOutsideTemp;
	/**
	 * Vorhaltezeit Aufheizen minuten
	 *
	 * Lead time heating up minutes
	 */
	public int heatUpTime; // in minutes
	/**
	 * Raumeinfluss
	 *
	 * Room impact
	 */
	public double roomImpact;
	/**
	 * Überhöhung Kessel
	 *
	 * Boiler superelevation
	 */
	public int boilerSuperelevation;
	/**
	 * Spreizung Heizkreis
	 *
	 * Spreading heating circuit
	 */
	public double heatingCircuitSpreading;
	/**
	 * Minimale Drehzahl Pumpe PHK %
	 *
	 * Minimum speed pump PHK %
	 */
	public int heatingPumpSpeedMin; // in %
	/**
	 * Drehzahl Pumpe PHK %
	 *
	 * Actual speed pump PHK %
	 */
	public int heatingPumpSpeedActual; // in %
	/**
	 * Mischer Laufzeit (minuten)
	 *
	 * Mixer runtime (minutes)
	 */
	public int mixerRuntime; // in minutes
	/**
	 * (Raumtemperatur Abgleich (* 10, neg. Werte sind um 1 zu hoch, 0 und -1 werden
	 * beide als 0 geliefert))
	 *
	 * (room temperature adjustment (* 10, neg. values are too high by 1, 0 and -1
	 * are both supplied as 0))
	 */
	public double roomTempCorrection;
	/**
	 * (Fusspunkt Fussbodenheizung)
	 *
	 * (Base point underfloor heating)
	 */
	public double underfloorHeatingBasePoint;
	/**
	 * (Steilheit Fussbodenheitzung)
	 *
	 * (Gradient underfloor heating)
	 */
	public double underfloorHeatingGradient;
	/**
	 * Warmwassertemperatur normal
	 *
	 * Hot water temperature normal
	 */
	public double hotWaterTempNormal;
	/**
	 * Warmwassertemperatur komfort
	 *
	 * Hot water temperature comfort
	 */
	public double hotWaterTempComfort;
	/**
	 * Warmwasser Aus=0, Normal=1, Komfort=2, Gesperrt=3 ???
	 *
	 * Hot water Off=0, Normal=1, Comfort=2, Locked=3 ???
	 */
	public int hotWaterOperationMode;
	public final String[] hotWaterOperationModes = { "off", "normal", "comfort", "locked" };
	/**
	 * Schaltdifferenz Warmwasser
	 *
	 * Hysteresis hot water
	 */
	public double hotWaterHysteresis;
	/**
	 * Maximale Warmwassertemperatur
	 *
	 * Maximum hot water temperature
	 */
	public double hotWaterTempMax;
	/**
	 * Nachlauf Pumpe PK/LP
	 *
	 * Overrun pump PK/LP
	 */
	public int heatingPumpOverrun;
	/**
	 * Maximale Puffer Temperatur
	 */
	public double bufferTempMax;
	/**
	 * Minimale Puffer Temperatur
	 *
	 * Minimum buffer temperature
	 */
	public double bufferTempMin;
	/**
	 * SchaltDifferenz Kessel
	 *
	 * Hysteresis boiler
	 */
	public double boilerHysteresis;
	/**
	 * Minimale Laufzeit Kessel (minuten)
	 *
	 * Minimum boiler running time (minutes)
	 */
	public int boilerOperationTime;
	/**
	 * Abschalt TA Kessel
	 *
	 * Shutdown TA boiler
	 */
	public double boilerShutdownTemp;
	/**
	 * Minimale Drehzahl Pumpe PK %
	 *
	 * Minimum speed pump PK %
	 */
	public int boilerPumpSpeedMin;
	/**
	 * (Nachlaufzeit Pumpe PZ)
	 *
	 * (Overrun pump PZ)
	 */
	public int circulationPumpOverrun;
	/**
	 * (Zirkulation Schaltdifferenz)
	 *
	 * (Circulation hysteresis)
	 */
	public double circulationHysteresis;
	/**
	 * Raumtemperatur ändern um
	 *
	 * Adjust room temperature by
	 */
	public double adjustRoomTempBy;
	/**
	 * Betriebszeit Kessel (Stunden)
	 *
	 * Boiler operating time (hours)
	 */
	public int boilerOperationTimeHours;
	/**
	 * Betriebszeit Kessel (Minuten)
	 *
	 * Boiler operating time (minutes)
	 */
	public int boilerOperationTimeMinutes;
	/**
	 * Anzahl Brennerstarts
	 *
	 * Number of burner starts
	 */
	public int burnerNumberOfStarts;
	/**
	 * Solare Leistung momentane Leistung der Solaranlage Die solare Leistung
	 * berechnet sich aus folgenden Messwerten: • Differenz zwischen der Temperatur
	 * am Kollektoraustritt und der Temperatur am Kollektoreintritt • Volumenstrom
	 * durch der Solaranlage
	 *
	 * Solar power Instantaneous power of the solar system The solar power is
	 * calculated from the following measured values: - difference between the
	 * temperature at the collector outlet and the temperature at the collector
	 * inlet - volume flow through the solar system
	 */
	public double solarPowerActual;
	/**
	 * (Tagesgewinn ???) die an diesem Tag bisher von der Solaranlage erzeugte
	 * Energiemenge Die Anzeige wird um Mitternacht selbsttätig auf 0 zurückgesetzt.
	 *
	 * (daily profit ???) The amount of energy generated by the solar system on this
	 * day so far. The display is automatically reset to 0 at midnight.
	 */
	public double solarGainDay;
	/**
	 * (Solargewinn gesamt???) die insgesamt von der Solaranlage erzeugte
	 * Energiemenge seit Inbetriebnahme der Solaranlage oder seit dem letzten
	 * Löschen des Solargewinns
	 *
	 * (total solar gain???) the total amount of energy generated by the solar
	 * system since commissioning of the solar system or since the last deletion of
	 * the solar gain.
	 */
	public double solarGainTotal;
	public int systemNumberOfStarts;
	public int circuit1LeadTime;
	public int circuit2LeadTime;
	public int circuit3LeadTime;
	/**
	 * Relais Heizkreispumpe = Relais &amp; 0x0001 Relais &amp; 0x0002 Relais &amp;
	 * 0x0004 Relais &amp; 0x0008 Relais &amp; 0x0010 Ladepumpe = Relais &amp;
	 * 0x0080 Zirkulationspumpe = Relais &amp; 0x0100 Kessel = Relais &amp; 0x0200
	 * Relais &amp; 0x0800 Brenner = Kessel &amp;&amp; (FLOW_TEMP_BOILER -
	 * RETURN_TEMP_BOILER > 2)
	 *
	 * Relay Heating circuit pump = relay &amp; 0x0001 Relay &amp; 0x0002 Relay
	 * &amp; 0x0004 Relay &amp; 0x0008 Relay &amp; 0x0010 Charge pump = Relay &amp;
	 * 0x0080 Circulation Pump = Relay &amp; 0x0100 Boiler = Relay &amp; 0x0200
	 * Relay &amp; 0x0800 Burner = Boiler &amp;&amp; (FLOW_TEMP_BOILER -
	 * RETURN_TEMP_BOILER > 2)
	 */
	public int relay;
	public static final int HEATING_PUMP_MASK = 0x0001;
	public static final int UNKNOWN_1_MASK = 0x0002;
	public static final int UNKNOWN_2_MASK = 0x0003;
	public static final int BURNER_MASK = 0x0004; // TODO verify this assumption
	public static final int MIXER_WARM_MASK = 0x0008;
	public static final int MIXER_COLD_MASK = 0x0010;
	public static final int CHARGE_PUMP_MASK = 0x0080;
	public static final int CIRCULATION_PUMP_MASK = 0x0100;
	public static final int BOILER_MASK = 0x0200;
	public static final int UNKNOWN_5_MASK = 0x0800;
	public static final int CHARGE_PUMP_LOG_BOILER_MASK = 0x1000; // TODO verify this assumption
	public static final int LED_BOILER_MASK = 0x2000; // TODO verify this assumption

	/**
	 * Heating circuit pump = relay &amp; 0x0001
	 */
	public boolean heatingPumpIsOn;
	/**
	 * Charge pump = Relay &amp; 0x0080
	 */
	public boolean chargePumpIsOn;
	/**
	 * Charge pump = Relay &amp; 0x1000
	 */
	public boolean logBoilderChargePumpIsOn;
	/**
	 * Charge pump = Relay &amp; 0x2000
	 */
	public boolean ledBoilerIsOn;
	/**
	 * Circulation Pump = Relay &amp; 0x0100
	 */
	public boolean circulationPumpIsOn;
	/**
	 * Boiler = Relay &amp; 0x0200
	 */
	public boolean boilerIsOn;
	/**
	 * Burner = Boiler &amp;&amp; (FLOW_TEMP_BOILER - RETURN_TEMP_BOILER &gt; 2) ||
	 * ((relay &amp; BURNER_MASK) != 0 )
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
	public int mixer1State;
	public final String[] mixerStateNames = { "off", "cool", "warm", "undef" };
	/**
	 * Relay &amp; 0x0800
	 */
	public boolean unknowRelayState5IsOn;

	/**
	 * Fehlerstatus (65535 = OK)
	 *
	 * Error status (65535 = OK)
	 */
	public int error;
	/**
	 * Betriebsart ???
	 *
	 * Operating mode ???
	 */
	public int operationModeX;
	/**
	 * Heizung aus=0; normal=1, komfort=2, abgesenkt=3 ???
	 *
	 * heating off=0; normal=1, comfort=2, lowering=3 ???
	 */
	public int heatingOperationModeX;
	/**
	 * Minimale Temperatur im oberen Bereich des Pufferspeichers Bei Heizen nur mit
	 * Scheitholzkessel? = Ja gilt folendes: Wenn die Temperatur am Temperaturfühler
	 * TPO bzw. PO KH die minimale Temperatur unterschreitet, sperrt er
	 * Heizungsregler die Heizkreise (Anfahrentlastung).
	 *
	 * Minimum temperature in the upper part of the storage tank When heating only with a * log boiler?
	 * log boiler? = Yes the following applies: If the temperature at the temperature sensor TPO or PO KH
	 * falls below the minimum temperature, the heating controller blocks the heating circuits (start-up).
	 * heating controller blocks the heating circuits (start-up unloading).
	 */
	public double logBoilerBufferTempMin;
	/**
	 * Minimale Temperatur des Scheitholzkessels 
	 * Der Scheitholzkessel darf die minimale Kesseltemperatur im Betrieb nicht unterschreiten,
	 * um Kondensatbildung im Kessel zu vermeiden. Die Temperatur wird am Temperaturfühler TV KH gemessen.
	 *
	 * Minimum temperature of the log boiler
	 * The log boiler must not fall below the minimum boiler temperature during operation, in order to
	 * avoid condensate formation in the boiler. The temperature is measured at temperature sensor TV KH. 
	 */
	public double logBoilerTempMin;
	/**
	 * Minimaler Sollwert für die Temperaturdifferenz zwischen Kesselvorlauf TVKH
	 * und Kesselrücklauf TRKH
	 * Der Heizungsregler vergleicht den Sollwert mit der tatsächlichen Temperaturdifferenz
	 * und schaltet die Pumpe des Scheitholzkessels ein oder aus.
	 * Wenn die Temperaturdifferenz 2 K kleiner ist als der Sollwert, schaltet die Pumpe ab.
	 * Wenn die Temperaturdifferenz größer ist als der Sollwert, schaltet die Pumpe ein.
	 *
	 * Minimum set point for the temperature difference between boiler flow TVKH
	 * and boiler return TRKH
	 * The heating controller compares the setpoint value with the
	 * actual temperature difference and switches the pump of the * log boiler on or off.
	 * log boiler on or off. If the temperature difference is 2 K less
	 * than the set point, the pump switches off. If the temperature difference is greater
	 * than the set point, the pump switches on.
     */
	public double logBoilerSpreadingMin;
	/**
	 * Minimale Drehzahl der Kesselpumpe PKH.
	 */
	public int logBoilerPumpSpeedMin;
	/**
	 * Ofen pumpe in %
	 *
	 * logBoiler pump in %
	 */
	public int logBoilerPumpSpeedActual;
	public int logBoilerSettings;
	public static final int LOG_BOILER_PARALLEL_OPERATION_MASK = 0x0008;
	public static final int BOILER_HEATS_BUFFER_MASK = 0x0010;
	/**
	 * Parallelbetrieb Nein - der Hauptkessel wird gesperrt, wenn der
	 * Scheitholzkessel in Betrieb ist Ja - der Hauptkessel wird nicht gesperrt,
	 * wenn der Scheitholzkessel in Betrieb ist
	 *
	 * Parallel operation No - the main boiler is blocked when the log boiler is in
	 * operation. Yes - the main boiler is not blocked when the log boiler is in
	 * operation.
	 */
	public boolean logBoilerParallelOperation;
	/**
	 * Einstellung, welche Anlagenvariante verwendet wird Nein - Anlagen mit
	 * Pufferspeicher, bei denen ausschließlich der Scheitholzkessel den Speicher
	 * erwärmt Ja - Anlagen mit Pufferspeicher/Kombispeicher, bei denen der
	 * Hauptkessel den Speicher erwärmt
	 *
	 * Setting which system variant is used. No - systems with buffer tank, where
	 * only the log boiler heats the tank. Yes - systems with storage tank/combined
	 * storage tank, where the main boiler heats the storage tank.
	 */
	public boolean boilerHeatsBuffer;

	public long timestamp;
	public String timestampString;

}
