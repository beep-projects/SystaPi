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
 * class representing the known indices of the SystaWeb UDP data packet. If new
 * fields are decoded, this class should be updated. The documentation is taken
 * from the German manual
 */
public final class SystaIndex {

	private SystaIndex() {
	} // Prevent instantiation

	/**
	 * Aussentemperatur gemessen am Temperaturfühler TA, an der Außenwand des
	 * Gebäudes
	 * <br/>
	 * Outside temperature measured at sensor TA.
	 */
	final static int OUTSIDE_TEMP = 0;
	/**
	 * Vorlauftemperatur Heizkreis 1 gemessen am Temperaturfühler TV, am Vorlauf von
	 * Heizkreis 1 (Rohrleitung, die zu den Heizkörpern hinführt) Anzeige erscheint
	 * nur bei Anlagen mit gemischtem Heizkreis.
	 * <br/>
	 * Flow temperature for heating circuit 1, measured at sensor TV. Displayed only for systems with a mixed heating circuit.
	 */
	final static int CIRCUIT_1_FLOW_TEMP = 1;
	/**
	 * Rücklauf Heizkreis TR Rücklauftemperatur Heizkreis 1 gemessen am
	 * Temperaturfühler TR, am Rücklauf von Heizkreis 1 (Rohrleitung, die von den
	 * Heizkörpern wegführt)
	 * <br/>
	 * Return temperature for heating circuit 1, measured at sensor TR.
	 */
	final static int CIRCUIT_1_RETURN_TEMP = 2;
	/**
	 * Warmwassertemperatur TWO Temperatur im oberen Bereich des
	 * Trinkwasserspeichers oder Kombispeichers, gemessen am Temperaturfühler TWO
	 * <br/>
	 * Hot water temperature TWO, temperature in the upper area of the drinking water
	 * or combination storage tank, measured at the TWO temperature sensor.
	 */
	final static int HOT_WATER_TEMP = 3;
	/**
	 * Puffertemperatur oben TPO gemessen am Temperaturfühler TPO Anlagen mit
	 * Pufferspeicher oder Kombispeicher: Temperatur im oberen Bereich des
	 * Pufferspeichers oder Kombispeichers Anlagen mit einstufigem Öl- oder
	 * Gaskessel: Temperatur im Kesselvorlauf des Heizkessels Anzeige erscheint nur
	 * bei Anlagen mit Pufferspeichern oder Kombispeichern und bei Anlagen mit
	 * einstufigen Öl- oder Gaskesseln
	 * <br/>
	 * Buffer temperature top TPO, measured at the TPO temperature sensor.
	 * For systems with buffer or combination storage: temperature in the upper area.
	 * For systems with single-stage oil/gas boiler: temperature in the boiler flow.
	 * Displayed only for systems with buffer/combi storage or single-stage oil/gas boilers.
	 */
	final static int BUFFER_TEMP_TOP = 4;
	/**
	 * Puffertemperatur unten TPU gemessen am Temperaturfühler TPU, Temperatur im
	 * unteren Bereich des Pufferspeichers oder Kombispeichers Anzeige erscheint nur
	 * bei Anlagen mit Pufferspeichern oder Kombispeichern.
	 * <br/>
	 * Buffer temperature bottom TPU, measured at the TPU temperature sensor,
	 * temperature in the lower area of the buffer or combination storage tank.
	 * Displayed only for systems with buffer or combination storage tanks.
	 */
	final static int BUFFER_TEMP_BOTTOM = 5;
	/**
	 * (Zirkulationstemperatur) gemessen am Temperaturfühler TZR am Rücklauf der
	 * Zirkulation Anzeige erscheint nur bei Anlagen mit Zirkulationskreis und wenn
	 * ein Temperaturfühler TRZ angeschlossen ist.
	 * <br/>
	 * (Circulation temperature) measured at the TZR temperature sensor at the
	 * return of the circulation. Displayed only for systems with a circulation
	 * circuit and if a TRZ temperature sensor is connected.
	 */
	final static int CIRCULATION_TEMP = 6;
	/**
	 * (Vorlauf Heizkreis 2 TV) Vorlauftemperatur Heizkreis 2 gemessen am
	 * Temperaturfühler TV2, am Vorlauf von Heizkreis 2 (Rohrleitung, die zu den
	 * Heizkörpern hinführt) Anzeige erscheint nur bei Anlagen mit 2 Heizkreisen
	 * <br/>
	 * (Flow heating circuit 2 TV) Flow temperature heating circuit 2 measured at
	 * the TV2 temperature sensor, at the flow of heating circuit 2 (pipe leading
	 * to the radiators). Displayed only for systems with 2 heating circuits.
	 */
	final static int CIRCUIT_2_FLOW_TEMP = 7;
	/**
	 * (Rücklauf Heizkreis 2 TR) Rücklauftemperatur Heizkreis 2 gemessen am
	 * Temperaturfühler TR2, am Rücklauf von Heizkreis 2 (Rohrleitung, die von den
	 * Heizkörpern wegführt) Anzeige erscheint nur bei Anlagen mit 2 Heizkreisen.
	 * <br/>
	 * (Return heating circuit 2 TR) Return temperature heating circuit 2 measured at
	 * temperature sensor TR2, at the return of heating circuit 2 (pipe leading away
	 * from radiators). Displayed only for systems with 2 heating circuits.
	 */
	final static int CIRCUIT_2_RETURN_TEMP = 8;
	/**
	 * Raumtemperatur HK1 ist gemessen am Bedienteil mit eingebautem
	 * Temperaturfühler TO
	 * <br/>
	 * Room temperature HK1 actual, measured at the control panel
	 * with built-in temperature sensor TO.
	 */
	final static int ROOM_TEMP_ACTUAL_1 = 9;
	/**
	 * Raumtemperatur HK2 gemessen am Bedienteil mit eingebautem Temperaturfühler TO
	 * <br/>
	 * Room temperature HK2 actual, measured at the control panel
	 * with built-in temperature sensor TO.
	 */
	final static int ROOM_TEMP_ACTUAL_2 = 10;
	/**
	 * (Kollektor ist) Kollektortemperatur Temperatur, gemessen am Temperaturfühler
	 * TSA im Kollektor
	 * <br/>
	 * (Collector actual) Collector temperature, temperature measured at the
	 * TSA temperature sensor in the collector.
	 */
	final static int COLLECTOR_TEMP_ACTUAL = 11;
	/**
	 * Vorlauf Kessel ist
	 * <br/>
	 * Boiler flow temperature actual.
	 */
	final static int BOILER_FLOW_TEMP = 12;
	/**
	 * Rücklauf Kessel ist
	 * <br/>
	 * Boiler return temperature actual.
	 */
	final static int BOILER_RETURN_TEMP = 13;
	/**
	 * Vorlauf Ofen ist
	 * <br/>
	 * Log boiler flow temperature actual.
	 */
	final static int LOG_BOILER_FLOW_TEMP = 14;
	/**
	 * Rücklauf Ofen ist
	 * <br/>
	 * Log boiler return temperature actual.
	 */
	final static int LOG_BOILER_RETURN_TEMP = 15;
	/**
	 * (Holzkessel Puffer oben)
	 * <br/>
	 * (Log boiler buffer top)
	 */
	final static int LOG_BOILER_BUFFER_TEMP_TOP = 16;
	/**
	 * (Schwimmbadtemperatur)
	 * <br/>
	 * (Swimming pool temperature)
	 */
	final static int SWIMMINGPOOL_TEMP = 17;
	/**
	 * (Vorlauf Schwimmbad)
	 * <br/>
	 * (Swimming pool flow temperature)
	 */
	final static int SWIMMINGPOOL_FLOW_TEMP = 18;
	/**
	 * (Rücklauf Schwimmbad)
	 * <br/>
	 * (Swimming pool return temperature)
	 */
	final static int SWIMMINGPOOL_RETURN_TEMP = 19;
	// final static int = 20
	// final static int = 21
	/**
	 * Sollwert Warmwassertemperatur aktuell gültiger Sollwert für die
	 * Warmwassertemperatur im Trinkwasserspeicher oder im oberen Bereich des
	 * Kombispeichers
	 * <br/>
	 * Setpoint for hot water temperature; currently valid setpoint for the
	 * hot water temperature in the drinking water storage or in the upper
	 * area of the combination storage.
	 */
	final static int HOT_WATER_TEMP_SET = 22;
	/**
	 * Sollwert Raumtemperatur aktuell gültiger Sollwert für die Raumtemperatur in
	 * Heizkreis 1
	 * <br/>
	 * Setpoint room temperature; currently valid setpoint for the room
	 * temperature in heating circuit 1.
	 */
	final static int ROOM_TEMP_SET_1 = 23;
	/**
	 * Vorlauf Heizkreis TV soll Sollwert Vorlauftemperatur Heizkreis 1 aktuell
	 * gültiger Sollwert für die Vorlauftemperatur in Heizkreis 1
	 * <br/>
	 * Flow heating circuit TV setpoint; Setpoint flow temperature heating circuit 1;
	 * currently valid setpoint for the flow temperature in heating circuit 1.
	 */
	final static int CIRCUIT_1_FLOW_TEMP_SET = 24;
	/**
	 * (Vorlauf Heizkreis 2 TV soll) Sollwert Vorlauftemperatur Heizkreis 2 aktuell
	 * gültiger Sollwert für die Vorlauftemperatur im Heizkreis 2 Anzeige erscheint
	 * nur bei Anlagen mit 2 Heizkreisen.
	 * <br/>
	 * (Flow heating circuit 2 TV setpoint) Setpoint flow temperature heating circuit 2;
	 * currently valid setpoint for the flow temperature in heating circuit 2.
	 * Displayed only for systems with 2 heating circuits.
	 */
	final static int CIRCUIT_2_FLOW_TEMP_SET = 25;
	/**
	 * (Raumtemperatur 2 ist) Sollwert Raumtemperatur Heizkreis 2 aktuell gültiger
	 * Sollwert für die Raumtemperatur in Heizkreis 2 Anzeige erscheint nur bei
	 * Anlagen mit 2 Heizkreisen
	 * <br/>
	 * (Room temperature 2 actual) Setpoint room temperature heating circuit 2;
	 * currently valid setpoint for the room temperature in heating circuit 2.
	 * Displayed only for systems with 2 heating circuits.
	 */
	final static int ROOM_TEMP_SET_2 = 26;
	/**
	 * (Vorlauf Heizkreis 2 TV soll)
	 */
	// final static int = 27
	// final static int = 28
	// final static int = 29
	// final static int = 30
	// final static int = 31
	// final static int = 32
	/**
	 * Sollwert Puffertemperatur aktuell gültiger Sollwert für die Puffertemperatur
	 * Anzeige erscheint nur bei Anlagen mit Pufferspeichern oder Kombispeichern.
	 * <br/>
	 * Setpoint buffer temperature; currently valid setpoint for the buffer temperature.
	 * Displayed only for systems with buffer or combination storage tanks.
	 */
	final static int BUFFER_TEMP_SET = 33;
	/**
	 * Kessel soll
	 * <br/>
	 * Boiler setpoint.
	 */
	final static int BOILER_TEMP_SET = 34;
	// final static int = 35
	/**
	 * Betriebsart
	 * <br/>
	 * Operating mode.
	 */
	final static int OPERATION_MODE = 36;
	/**
	 * ???
	 */
	// final static int = 37
	/**
	 * ???
	 */
	// final static int = 38
	/**
	 * Raumtemperatur normal (soll)
	 * <br/>
	 * Room temperature normal (setpoint).
	 */
	final static int ROOM_TEMP_SET_NORMAL = 39;
	/**
	 * Raumtemperatur komfort (soll)
	 * <br/>
	 * Room temperature comfort (setpoint).
	 */
	final static int ROOM_TEMP_SET_COMFORT = 40;
	/**
	 * Raumtemperatur abgesenkt (soll)
	 * <br/>
	 * Room temperature lowering (setpoint).
	 */
	final static int ROOM_TEMP_SET_LOWERING = 41;
	/**
	 * Heizung aus=0, normal=1, absenken=3
	 * <br/>
	 * Heating: off=0, normal=1, lowering=3.
	 */
	final static int HEATING_OPERATION_MODE = 42; // off=0, normal=1, lowering=3
	// final static int = 43
	// final static int = 44
	// final static int = 45
	// final static int = 46
	/**
	 * Regelung HK nach: 0=Aussentemperatur, 1=Raumtemperatur, 2= TA/TI kombiniert
	 * <br/>
	 * Heating circuit controlled by: 0=outside temperature, 1=room temperature, 2=TA/TI combined.
	 */
	final static int CONTROLLED_BY = 47; // 0=external temp, 1=room temp, 2= ext./room combined
	/**
	 * Fusspunkt
	 * <br/>
	 * Base point
	 */
	final static int HEATING_CURVE_BASE_POINT = 48;
	/**
	 * Heizkurvenoptimierung?
	 */
	// final static int = 49;
	/**
	 * Steilheit
	 * <br/>
	 * Gradient
	 */
	final static int HEATING_CURVE_GRADIENT = 50; // K/K
	/**
	 * Heizkurvenoptimierung?
	 */
	// final static int = 51
	/**
	 * Maximale Vorlauf Temperatur
	 * <br/>
	 * Maximum flow temperature
	 */
	final static int MAX_FLOW_TEMP = 52;
	/**
	 * Heizgrenze Heizbetrieb
	 * <br/>
	 * Heating limit for heating mode
	 */
	final static int HEATING_LIMIT_TEMP = 53;
	/**
	 * Heizgrenze Absenken
	 * <br/>
	 * Heating limit lowering mode
	 */
	final static int HEATING_LIMIT_TEMP_LOWERING = 54;
	/**
	 * Frostschutz Aussentemperatur
	 * <br/>
	 * Antifreeze outside temperature
	 */
	final static int ANTI_FREEZE_OUTSIDE_TEMP = 55;
	/**
	 * Vorhaltezeit Aufnormal minuten
	 * <br/>
	 * Lead time for normal heating, minutes
	 */
	final static int HEAT_UP_TIME = 56; // in minutes
	/**
	 * Raumeinfluss
	 * <br/>
	 * Room influence
	 */
	final static int ROOM_IMPACT = 57; // in K/K
	/**
	 * Ueberhoehung Kessel
	 * <br/>
	 * Boiler superelevation
	 */
	final static int BOILER_SUPERELEVATION = 58; // in K
	/**
	 * Spreizung Heizkreis
	 * <br/>
	 * Heating circuit spread
	 */
	final static int HEATING_CIRCUIT_SPREADING = 59; // in K
	/**
	 * Minimale Drehzahl Pumpe PHK %
	 * <br/>
	 * Minimum speed pump PHK %
	 */
	final static int HEATING_PUMP_SPEED_MIN = 60; // in %
	// final static int = 61
	/**
	 * Mischer Laufzeit (minuten)
	 * <br/>
	 * Mixer runtime (minutes)
	 */
	final static int MIXER_RUNTIME = 62; // in minutes
	// final static int = 63 //(proportionalbereich?)
	// final static int = 64 //(nachstellzeit?)
	/**
	 * (Raumtemperatur Abgleich (* 10, neg. Werte sind um 1 zu hoch, 0 und -1 werden
	 * beide als 0 geliefert))
	 * <br/>
	 * (Room temperature adjustment (* 10, negative values are too high by 1, 0 and -1 are both delivered as 0))
	 */
	final static int ROOM_TEMP_CORRECTION = 65;
	// final static int = 66
	// final static int = 67
	// final static int = 68
	// final static int = 69
	// final static int = 70
	// final static int = 71
	// final static int = 72
	// final static int = 73
	// final static int = 74
	/**
	 * ???
	 */
	// final static int = 75
	// final static int = 76
	// final static int = 77
	// final static int = 78
	// final static int = 79
	// final static int = 80
	// final static int = 81
	// final static int = 82
	// final static int = 83
	// final static int = 84
	// final static int = 85
	// final static int = 86
	/**
	 * (Fusspunkt Fussbodenheizung)
	 * <br/>
	 * (Base point underfloor heating)
	 */
	final static int UNDERFLOOR_HEATING_BASE_POINT = 87;
	// final static int = 88
	/**
	 * (Steilheit Fussbodenheitzung)
	 * <br/>
	 * (Gradient underfloor heating)
	 */
	final static int UNDERFLOOR_HEATING_GRADIENT = 89;
	// final static int = 90
	// final static int = 91
	// final static int = 92
	// final static int = 93
	// final static int = 94
	// final static int = 95
	// final static int = 96
	// final static int = 97
	// final static int = 98
	// final static int = 99
	// final static int = 100
	// final static int = 101
	// final static int = 102
	// final static int = 103
	// final static int = 104
	// final static int = 105
	// final static int = 106
	// final static int = 107
	// final static int = 108
	// final static int = 109
	// final static int = 110
	// final static int = 111
	// final static int = 112
	// final static int = 113
	/**
	 * ???
	 */
	// final static int = 114
	// final static int = 115
	// final static int = 116
	// final static int = 117
	// final static int = 118
	// final static int = 119
	// final static int = 120
	// final static int = 121
	// final static int = 122
	// final static int = 123
	// final static int = 124
	// final static int = 125
	// final static int = 126
	// final static int = 127
	// final static int = 128
	// final static int = 129
	// final static int = 130
	// final static int = 131
	// final static int = 132
	// final static int = 133
	// final static int = 134
	// final static int = 135
	// final static int = 136
	// final static int = 137
	// final static int = 138
	// final static int = 139
	// final static int = 140
	// final static int = 141
	// final static int = 142
	// final static int = 143
	// final static int = 144
	// final static int = 145
	// final static int = 146
	// final static int = 147
	// final static int = 148
	/**
	 * Warmwassertemperatur normal
	 * <br/>
	 * Hot water temperature normal
	 */
	final static int HOT_WATER_TEMP_NORMAL = 149;
	/**
	 * Warmwassertemperatur komfort
	 * <br/>
	 * Hot water temperature comfort
	 */
	final static int HOT_WATER_TEMP_COMFORT = 150;
	/**
	 * Warmwasser Normal=1, Komfort=2, Gesperrt=3 ???
	 * <br/>
	 * Hot water Normal=1, Comfort=2, Locked=3 ???
	 */
	final static int HOT_WATER_OPERATION_MODE = 151;
	// final static int = 152
	// final static int = 153
	// final static int = 154
	/**
	 * Schaltdifferenz Warmwasser
	 * <br/>
	 * Switching difference hot water
	 */
	final static int HOT_WATER_HYSTERESIS = 155;
	/**
	 * Maximale Warmwassertemperatur
	 * <br/>
	 * Maximum hot water temperature
	 */
	final static int HOT_WATER_TEMP_MAX = 156;
	/**
	 * 0 = "OPTIMA/EXPRESSO" 1 = "TITAN" 2 = "Puffer und ULV" 3 = "Puffer + LP" 4 =
	 * "Expressino" 5 = "Puffer u. Frischwasserstation"
	 * <br/>
	 * 0 = "OPTIMA/EXPRESSO" 1 = "TITAN" 2 = "Buffer and ULV" 3 = "Buffer + LP" 4 =
	 * "Expressino" 5 = "Buffer and fresh water station"
	 */
	final static int BUFFER_TYPE = 157;
	/**
	 * Nachlauf Pumpe PK/LP
	 * <br/>
	 * Pump overrun PK/LP
	 */
	final static int PUMP_OVERRUN = 158;
	/**
	 * Maximale Puffer Temperatur
	 * <br/>
	 * Maximum buffer temperature
	 */
	final static int BUFFER_TEMP_MAX = 159;
	/**
	 * Minimale Puffer Temperatur
	 * <br/>
	 * Minimum buffer temperature
	 */
	final static int BUFFER_TEMP_MIN = 160;
	/**
	 * SchaltDifferenz Kessel
	 * <br/>
	 * Switching difference boiler
	 */
	final static int BOILER_HYSTERESIS = 161;
	/**
	 * Minimale Laufzeit Kessel (minuten)
	 * <br/>
	 * Minimum boiler runtime (minutes)
	 */
	final static int BOILER_RUNTIME_MIN = 162;
	/**
	 * Abschalt TA Kessel
	 * <br/>
	 * Shutdown TA boiler
	 */
	final static int BOILER_SHUTDOWN_TEMP = 163;
	/**
	 * Minimale Drehzahl Pumpe PK %
	 * <br/>
	 * Minimum speed pump PK %
	 */
	final static int BOILER_PUMP_SPEED_MIN = 164;
	// final static int = 165
	// final static int = 166
	// final static int = 167
	// final static int = 168
	/**
	 * (Nachlaufzeit Pumpe PZ)
	 * <br/>
	 * (Overrun time pump PZ)
	 */
	final static int CIRCULATION_PUMP_OVERRUN = 169;
	/**
	 * (Sperrzeit Taster)
	 * <br/>
	 * (Lockout time push button)
	 */
	final static int CIRCULATION_LOCKOUT_TIME_PUSH_BUTTON = 170; // in min
	/**
	 * (Zirkulation Schaltdifferenz)
	 * <br/>
	 * (Circulation switching difference)
	 */
	final static int CIRCULATION_HYSTERESIS = 171;
	// final static int = 172
	// final static int = 173
	// final static int = 174
	/**
	 * Einstellungen Holzkessel
	 * <br/>
	 * Log boiler settings
	 */
	final static int LOG_BOILER_SETTINGS = 175;
	/**
	 * Raumtemperatur ändern um
	 * <br/>
	 * Adjust room temperature by
	 */
	final static int ADJUST_ROOM_TEMP_BY = 176;
	// final static int = 177
	// final static int = 178
	/**
	 * Betriebszeit Kessel (Stunden)
	 * <br/>
	 * Boiler operating time (hours)
	 */
	final static int BOILER_OPERATION_TIME_HOURS = 179;
	/**
	 * Betriebszeit Kessel (Minuten)
	 * <br/>
	 * Boiler operating time (minutes)
	 */
	final static int BOILER_OPERATION_TIME_MINUTES = 180;
	/**
	 * Anzahl Brennerstarts
	 * <br/>
	 * Number of burner starts
	 */
	final static int BURNER_NUMBER_OF_STARTS = 181;
	/**
	 * Solare Leistung
	 * <br/>
	 * Solar power actual
	 */
	final static int SOLAR_POWER_ACTUAL = 182;
	/**
	 * Tagesgewinn
	 * <br/>
	 * Daily gain
	 */
	final static int SOLAR_GAIN_DAY = 183;
	/**
	 * Solargewinn gesamt
	 * <br/>
	 * Total solar gain
	 */
	final static int SOLAR_GAIN_TOTAL = 184;
	/**
	 * Systemstarts
	 * <br/>
	 * System starts
	 */
	final static int SYSTEM_NUMBER_OF_STARTS = 185;
	/**
	 * Vorhaltezeit 1
	 * <br/>
	 * Lead time 1
	 */
	final static int CIRCUIT_1_LEAD_TIME = 186;
	/**
	 * Vorhaltezeit 2
	 * <br/>
	 * Lead time 2
	 */
	final static int CIRCUIT_2_LEAD_TIME = 187;
	/**
	 * Vorhaltezeit 3
	 * <br/>
	 * Lead time 3
	 */
	final static int CIRCUIT_3_LEAD_TIME = 188;
	/**
	 * Minimale Temperatur im oberen Bereich des Pufferspeichers
	 * <br/>
	 * Minimum temperature in the upper area of the buffer storage
	 */
	final static int LOG_BOILER_BUFFER_TEMP_MIN = 189;
	/**
	 * Minimale Temperatur des Scheitholzkessels
	 * <br/>
	 * Minimum temperature of the log boiler
	 */
	final static int LOG_BOILER_TEMP_MIN = 190;
	/**
	 * Minimaler Sollwert für die Temperaturdifferenz zwischen Kesselvorlauf TVKH
	 * und Kesselrücklauf TRKH
	 * <br/>
	 * Minimum setpoint for the temperature difference between boiler flow TVKH
	 * and boiler return TRKH
	 */
	final static int LOG_BOILER_SPREADING_MIN = 191;
	/**
	 * Minimale Drehzahl der Kesselpumpe PKH.
	 * <br/>
	 * Minimum speed of the boiler pump PKH.
	 */
	final static int LOG_BOILER_PUMP_SPEED_MIN = 192;
	// final static int = 190
	// final static int = 191
	// final static int = 192
	/**
	 * ???
	 */
	// final static int = 193
	// final static int = 194
	// final static int = 195
	// final static int = 196
	// final static int = 197
	// final static int = 198
	// final static int = 199
	// final static int = 200
	// final static int = 201
	// final static int = 202
	// final static int = 203
	// final static int = 204
	// final static int = 205
	// final static int = 206
	// final static int = 207
	// final static int = 208
	// final static int = 209
	// final static int = 210
	// final static int = 211
	// final static int = 212
	// final static int = 213
	// final static int = 214
	// final static int = 215
	// final static int = 216
	// final static int = 217
	// final static int = 218
	// final static int = 219
	/**
	 * Relais
	 * <br/>
	 * Relay
	 */
	final static int RELAY = 220;
	/**
	 * Heizkreispumpe Geschwindigkeit x*5%
	 * <br/>
	 * Heating circuit pump speed x*5%
	 */
	final static int HEATING_PUMP_SPEED_ACTUAL = 221;
	/**
	 * Status ???
	 */
	// final static int = 222
	// final static int = 223
	// final static int = 224
	// final static int = 225
	// final static int = 226
	/**
	 * Kesselpumpe Geschwindigkeit x*5%
	 * <br/>
	 * Boiler pump speed x*5%
	 */
	final static int BOILER_PUMP_SPEED_ACTUAL = 227;
	/**
	 * Fehlerstatus (65535 = OK)
	 * <br/>
	 * Error status (65535 = OK)
	 */
	final static int ERROR = 228;
	// final static int = 229
	/**
	 * Betriebsart ???
	 * <br/>
	 * Operating mode ???
	 */
	final static int OPERATION_MODE_X = 230;
	/**
	 * Heizung aus=0; normal=1, komfort=2, abgesenkt=3 ???
	 * <br/>
	 * Heating off=0; normal=1, comfort=2, lowered=3 ???
	 */
	final static int HEATING_OPERATION_MODE_X = 231;
	/**
	 * Status HK 1 0="Aus" 1="Aus Heizgrenze" 2="Aus TI" 3="Gesperrt TPO" 4="Aus
	 * WW-Vorrang" 5="Ein" 6="Frostschutz" 7=,"K\u00fchlen" 8="Vorhaltezeit"
	 * 9="Heizbetrieb" 10="Komfortbetrieb" 11="Absenkbetrieb" 12="Aus TSB"
	 * 13="Gesperrt" 14="Normal" 15="Erh\u00f6ht" 16="WW-Modus" 17="Estrich
	 * trocknen" 18="K\u00fchlbetrieb"
	 * <br/>
	 * Status HC 1 0="Off" 1="Off heating limit" 2="Off TI" 3="Locked TPO" 4="Off
	 * DHW priority" 5="On" 6="Frost protection" 7="Cooling" 8="Lead time"
	 * 9="Heating mode" 10="Comfort mode" 11="Setback mode" 12="Off TSB"
	 * 13="Locked" 14="Normal" 15="Elevated" 16="DHW mode" 17="Screed
	 * drying" 18="Cooling mode"
	 */
	final static int CIRCUIT_1_OPERATION_MODE = 232;
	// final static int = 233
	// final static int = 234
	// final static int = 235
	// final static int = 236
	// final static int = 237
	// final static int = 238
	// final static int = 239
	// final static int = 240
	// final static int = 241
	// final static int = 242
	// final static int = 243
	// final static int = 244
	// final static int = 245
	/**
	 * Ofenpumpe Geschwindigkeit x*5%
	 * <br/>
	 * Log boiler pump speed x*5%
	 */
	final static int LOG_BOILER_PUMP_SPEED_ACTUAL = 246;
	/**
	 * Status Holzkessel
	 * <br/>
	 * Status log boiler
	 */
	final static int LOG_BOILER_OPERATION_MODE = 247;
	/**
	 * Status Kessel
	 * <br/>
	 * Status boiler
	 */
	final static int BOILER_OPERATION_MODE = 248;
	/**
	 * Status Zirkulation 0="Aus" 1="Nachlauf" 2="Sperrzeit" 3="Gesperrt" 4="Aus
	 * F\u00fchler TZR" 5="Ein" 6="Frost"
	 * <br/>
	 * Status circulation 0="Off" 1="Overrun" 2="Lock time" 3="Locked" 4="Off
	 * sensor TZR" 5="On" 6="Frost"
	 */
	final static int CIRCULATION_OPERATION_MODE = 249;
}
