package de.freaklamarsch.systarest;

/**
 * Class for holding the status of a Paradigma SystaComfort II heating controller.
 * The status is build from all the known fields from a SystaWeb UDP data packet.
 * If new fields are decoded, this class should be updated.
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
	 * Flow stove
	 */
	public double stoveFlowTemp;
	/**
	 * Rücklauf Ofen ist
	 * 
	 * Return stove
	 */
	public double stoveReturnTemp;
	/**
	 * (Holzkessel Puffer oben)
	 * 
	 * (wood boiler buffer top)
	 */
	public double woodBoilerBufferTempTop;
	/**
	 * (Schwimmbadtemperatur)
	 * 
	 * (swimming pool temperature)
	 */
	public double swimmingpoolTemp;
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
	 * Betriebsart 0 = Auto Prog. 1 1 = Auto Prog. 2 2 = Auto Prog. 3 3 = Dauernd
	 * Normal 4 = Dauernd Komfort 5 = Dauernd Absenken 6 = Sommer 7 = Aus 8 = Party
	 * 14= Test oder Kaminfeger
	 * 
	 * • Automatik 1, 2 oder 3 - Anlage läuft im Regelbetrieb, gemäß den
	 * Einstellungen im Zeitprogramm 1, 2 oder 3 Trinkwassererwärmung und
	 * Zirkulation laufen entsprechend den Einstellungen im jeweiligen Zeitprogramm.
	 * • Dauernd Normal - Heizkreis wird auf „Sollwert Raumtemperatur Normal“
	 * geregelt, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm. • Dauernd Komfort - Heizkreis wird
	 * auf „Sollwert Raumtemperatur Komfort“ geregelt, Trinkwassererwärmung und
	 * Zirkulation laufen entsprechend den Einstellungen im jeweiligen Zeitprogramm.
	 * • Dauernd Abgesenkt - Heizkreis wird auf „Sollwert Raumtemperatur Abgesenkt“
	 * geregelt, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm. • Sommer - Heizung ist
	 * ausgeschaltet, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm. • Aus - Heizung,
	 * Trinkwassererwärmung und Zirkulation sind ausgeschaltet, Frostschutz ist
	 * sichergestellt • Party - Unabhängig vom Heizzeitprogramm verwendet der Regler
	 * den „Sollwert Raumtemperatur Normal“. Unabhängig vom Warmwasserzeitprogramm
	 * verwendet der Regler den „Sollwert Warmwassertemperatur Normal“. Die
	 * Zirkulation ist freigegeben.
	 * 
	 * Operating mode 0 = Auto Prog. 1 1 = Auto Prog. 2 2 = Auto Prog. 3 3 =
	 * Continuous Normal 4 = Continuous Comfort 5 = Continuous Lowering 6 = Summer 7
	 * = Off 8 = Party 14= Test or chimney sweep
	 * 
	 * • Automatic 1, 2 or 3 - System runs in control mode, according to the
	 * settings in time program 1, 2 or 3 DHW heating and circulation run according
	 * to the settings in the respective time program. • Continuous Normal - Heating
	 * circuit is controlled to "Setpoint room temperature normal", DHW heating and
	 * circulation run according to the settings in the respective time program. •
	 * Continuous Comfort - Heating circuit is controlled to "Comfort room
	 * temperature setpoint", DHW heating and circulation run according to the
	 * settings in the respective time program. • Continuous Lowering - Heating
	 * circuit is controlled to "Room temperature setpoint reduced", drinking water
	 * heating and circulation run according to the settings in the respective time
	 * program. • Summer - Heating is switched off, drinking water heating and
	 * circulation run according to the settings in the respective time program. •
	 * Off - Heating, drinking water heating and circulation are switched off, frost
	 * protection is ensured. • Party - Independent of the heating time program, the
	 * controller uses the "Setpoint room temperature normal". Independent of the
	 * hot water time program, the controller uses the "Setpoint hot water
	 * temperature normal". Circulation is enabled.
	 */
	public int operationMode;
	public final String[] operationModes = { "Auto Prog. 1", "Auto Prog. 2", "Auto Prog. 3", "Continuous Normal",
			"Continuous Comfort", "Continuous Lowering", "Summer", "Off", "Party", "undef", "undef", "undef", "undef",
			"undef", "undef", "Test" };
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
	public double spreadingHeatingCircuit;
	/**
	 * Minimale Drehzahl Pumpe PHK %
	 * 
	 * Minimum speed pump PHK %
	 */
	public int heatingMinSpeedPump; // in %
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
	public int pumpOverrun;
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
	public int boilerMinSpeedPump;
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
	public int numberBurnerStarts;
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
	public int countdown;
	/**
	 * Relais Heizkreispumpe = Relais &amp; 0x0001 Relais &amp; 0x0002 Relais &amp; 0x0004
	 * Relais &amp; 0x0008 Relais &amp; 0x0010 Ladepumpe = Relais &amp; 0x0080 Zirkulationspumpe
	 * = Relais &amp; 0x0100 Kessel = Relais &amp; 0x0200 Relais &amp; 0x0800 Brenner = Kessel
	 * &amp;&amp; (FLOW_TEMP_BOILER - RETURN_TEMP_BOILER > 2)
	 * 
	 * Relay Heating circuit pump = relay &amp; 0x0001 Relay &amp; 0x0002 Relay &amp; 0x0004
	 * Relay &amp; 0x0008 Relay &amp; 0x0010 Charge pump = Relay &amp; 0x0080 Circulation Pump =
	 * Relay &amp; 0x0100 Boiler = Relay &amp; 0x0200 Relay &amp; 0x0800 Burner = Boiler &amp;&amp;
	 * (FLOW_TEMP_BOILER - RETURN_TEMP_BOILER > 2)
	 */
	public int relay;
	public static final int HEATING_PUMP_MASK = 0x0001;
	public static final int CHARGE_PUMP_MASK = 0x0080;
	public static final int CIRCULATION_PUMP_MASK = 0x0100;
	public static final int BOILER_MASK = 0x0200;
	public static final int UNKNOWN_1_MASK = 0x0002;
	public static final int UNKNOWN_2_MASK = 0x0003;
	public static final int UNKNOWN_3_MASK = 0x0008;
	public static final int UNKNOWN_4_MASK = 0x0010;
	public static final int UNKNOWN_5_MASK = 0x0800;
	/**
	 * Heating circuit pump = relay &amp; 0x0001
	 */
	public boolean heatingPumpIsOn;
	/**
	 * Charge pump = Relay &amp; 0x0080
	 */
	public boolean chargePumpIsOn;
	/**
	 * Circulation Pump = Relay &amp; 0x0100
	 */
	public boolean circulationPumpIsOn;
	/**
	 * Boiler = Relay &amp; 0x0200
	 */
	public boolean boilerIsOn;
	/**
	 * Burner = Boiler &amp;&amp; (FLOW_TEMP_BOILER - RETURN_TEMP_BOILER &gt; 2)
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
	public boolean unknowRelayState3IsOn;
	/**
	 * Relay &amp; 0x0010
	 */
	public boolean unknowRelayState4IsOn;
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
	 * Ofen pumpe ???
	 * 
	 * Stove pump ???
	 */
	public int stovePumpSpeedActual;
	public long timestamp;
	public String timestampString;

}
