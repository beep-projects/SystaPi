package de.freaklamarsch.systarest;

/**
 * class representing the known indices of the SystaWeb UDP data packet.
 * If new fields are decoded, this class should be updated.
 * The documentation is taken from the German manual
 */
public final class SystaIndex {

	private SystaIndex() {
	} // Prevent instantiation

	/**
	 * Aussentemperatur gemessen am Temperaturfühler TA, an der Außenwand des
	 * Gebäudes
	 */
	final static int OUTSIDE_TEMP = 0;
	/**
	 * Vorlauftemperatur Heizkreis 1 gemessen am Temperaturfühler TV, am Vorlauf von
	 * Heizkreis 1 (Rohrleitung, die zu den Heizkörpern hinführt) Anzeige erscheint
	 * nur bei Anlagen mit gemischtem Heizkreis.
	 */
	final static int CIRCUIT_1_FLOW_TEMP = 1;
	/**
	 * Rücklauf Heizkreis TR Rücklauftemperatur Heizkreis 1 gemessen am
	 * Temperaturfühler TR, am Rücklauf von Heizkreis 1 (Rohrleitung, die von den
	 * Heizkörpern wegführt)
	 */
	final static int CIRCUIT_1_RETURN_TEMP = 2;
	/**
	 * Warmwassertemperatur TWO Temperatur im oberen Bereich des
	 * Trinkwasserspeichers oder Kombispeichers, gemessen am Temperaturfühler TWO
	 */
	final static int HOT_WATER_TEMP = 3;
	/**
	 * Puffertemperatur oben TPO gemessen am Temperaturfühler TPO Anlagen mit
	 * Pufferspeicher oder Kombispeicher: Temperatur im oberen Bereich des
	 * Pufferspeichers oder Kombispeichers Anlagen mit einstufigem Öl- oder
	 * Gaskessel: Temperatur im Kesselvorlauf des Heizkessels Anzeige erscheint nur
	 * bei Anlagen mit Pufferspeichern oder Kombispeichern und bei Anlagen mit
	 * einstufigen Öl- oder Gaskesseln
	 */
	final static int BUFFER_TEMP_TOP = 4;
	/**
	 * Puffertemperatur unten TPU gemessen am Temperaturfühler TPU, Temperatur im
	 * unteren Bereich des Pufferspeichers oder Kombispeichers Anzeige erscheint nur
	 * bei Anlagen mit Pufferspeichern oder Kombispeichern.
	 */
	final static int BUFFER_TEMP_BOTTOM = 5;
	/**
	 * (Zirkulationstemperatur) gemessen am Temperaturfühler TZR am Rücklauf der
	 * Zirkulation Anzeige erscheint nur bei Anlagen mit Zirkulationskreis und wenn
	 * ein Temperaturfühler TRZ angeschlossen ist.
	 */
	final static int CIRCULATION_TEMP = 6;
	/**
	 * (Vorlauf Heizkreis 2 TV) Vorlauftemperatur Heizkreis 2 gemessen am
	 * Temperaturfühler TV2, am Vorlauf von Heizkreis 2 (Rohrleitung, die zu den
	 * Heizkörpern hinführt) Anzeige erscheint nur bei Anlagen mit 2 Heizkreisen
	 */
	final static int CIRCUIT_2_FLOW_TEMP = 7;
	/**
	 * (Rücklauf Heizkreis 2 TR) Rücklauftemperatur Heizkreis 2 gemessen am
	 * Temperaturfühler TR2, am Rücklauf von Heizkreis 2 (Rohrleitung, die von den
	 * Heizkörpern wegführt) Anzeige erscheint nur bei Anlagen mit 2 Heizkreisen.
	 */
	final static int CIRCUIT_2_RETURN_TEMP = 8;
	/**
	 * Raumtemperatur HK1 ist gemessen am Bedienteil mit eingebautem
	 * Temperaturfühler TO
	 */
	final static int ROOM_TEMP_ACTUAL_1 = 9;
	/**
	 * Raumtemperatur HK2 gemessen am Bedienteil mit eingebautem Temperaturfühler TO
	 */
	final static int ROOM_TEMP_ACTUAL_2 = 10;
	/**
	 * (Kollektor ist) Kollektortemperatur Temperatur, gemessen am Temperaturfühler
	 * TSA im Kollektor
	 */
	final static int COLLECTOR_TEMP_ACTUAL = 11;
	/**
	 * Vorlauf Kessel ist
	 */
	final static int BOILER_FLOW_TEMP = 12;
	/**
	 * Rücklauf Kessel ist
	 */
	final static int BOILER_RETURN_TEMP = 13;
	/**
	 * Vorlauf Ofen ist
	 */
	final static int STOVE_FLOW_TEMP = 14;
	/**
	 * Rücklauf Ofen ist
	 */
	final static int STOVE_RETURN_TEMP = 15;
	/**
	 * (Holzkessel Puffer oben)
	 */
	final static int WOOD_BOILER_BUFFER_TEMP_TOP = 16;
	/**
	 * (Schwimmbadtemperatur)
	 */
	final static int SWIMMINGPOOL_TEMP = 17;
	/**
	 * (Vorlauf Schwimmbad)
	 */
	final static int SWIMMINGPOOL_FLOW_TEMP = 18;
	/**
	 * (Rücklauf Schwimmbad)
	 */
	final static int SWIMMINGPOOL_RETURN_TEMP = 19;
	// final static int = 20
	// final static int = 21
	/**
	 * Sollwert Warmwassertemperatur aktuell gültiger Sollwert für die
	 * Warmwassertemperatur im Trinkwasserspeicher oder im oberen Bereich des
	 * Kombispeichers
	 */
	final static int HOT_WATER_TEMP_SET = 22;
	/**
	 * Sollwert Raumtemperatur aktuell gültiger Sollwert für die Raumtemperatur in
	 * Heizkreis 1
	 */
	final static int ROOM_TEMP_SET_1 = 23;
	/**
	 * Vorlauf Heizkreis TV soll Sollwert Vorlauftemperatur Heizkreis 1 aktuell
	 * gültiger Sollwert für die Vorlauftemperatur in Heizkreis 1
	 */
	final static int CIRCUIT_1_FLOW_TEMP_SET = 24;
	/**
	 * (Vorlauf Heizkreis 2 TV soll) Sollwert Vorlauftemperatur Heizkreis 2 aktuell
	 * gültiger Sollwert für die Vorlauftemperatur im Heizkreis 2 Anzeige erscheint
	 * nur bei Anlagen mit 2 Heizkreisen.
	 */
	final static int CIRCUIT_2_FLOW_TEMP_SET = 25;
	/**
	 * (Raumtemperatur 2 ist) Sollwert Raumtemperatur Heizkreis 2 aktuell gültiger
	 * Sollwert für die Raumtemperatur in Heizkreis 2 Anzeige erscheint nur bei
	 * Anlagen mit 2 Heizkreisen
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
	 */
	final static int BUFFER_TEMP_SET = 33;
	/**
	 * Kessel soll
	 */
	final static int BOILER_TEMP_SET = 34;
	// final static int = 35
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
	 * • Automatik 1, 2 oder 3 - Anlage läuft im Regelbetrieb, gemäß den Einstellungen im Zeitprogramm 1, 2 oder 3 Trinkwassererwärmung und
	 * Zirkulation laufen entsprechend den Einstellungen im jeweiligen Zeitprogramm.
	 * 
	 * • Dauernd Normal - Heizkreis wird auf „Sollwert Raumtemperatur Normal“ geregelt, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm. 
	 * 
	 * • Dauernd Komfort - Heizkreis wird auf „Sollwert Raumtemperatur Komfort“ geregelt, Trinkwassererwärmung und
	 * Zirkulation laufen entsprechend den Einstellungen im jeweiligen Zeitprogramm.
	 * 
	 * • Dauernd Abgesenkt - Heizkreis wird auf „Sollwert Raumtemperatur Abgesenkt“
	 * geregelt, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm. 
	 * 
	 * • Sommer - Heizung ist ausgeschaltet, Trinkwassererwärmung und Zirkulation laufen entsprechend den
	 * Einstellungen im jeweiligen Zeitprogramm. 
	 * 
	 * • Aus - Heizung, Trinkwassererwärmung und Zirkulation sind ausgeschaltet, Frostschutz ist
	 * sichergestellt 
	 * 
	 * • Party - Unabhängig vom Heizzeitprogramm verwendet der Regler den „Sollwert Raumtemperatur Normal“. 
	 * Unabhängig vom Warmwasserzeitprogramm  verwendet der Regler den „Sollwert Warmwassertemperatur Normal“. Die
	 * Zirkulation ist freigegeben.
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
	 */
	final static int ROOM_TEMP_SET_NORMAL = 39;
	/**
	 * Raumtemperatur komfort (soll)
	 */
	final static int ROOM_TEMP_SET_COMFORT = 40;
	/**
	 * Raumtemperatur abgesenkt (soll)
	 */
	final static int ROOM_TEMP_SET_LOWERING = 41;
	/**
	 * Heizung aus=0, normal=1, absenken=3
	 */
	final static int HEATING_OPERATION_MODE = 42; // off=0, normal=1, lowering=3
	// final static int = 43
	// final static int = 44
	// final static int = 45
	// final static int = 46
	/**
	 * Regelung HK nach: 0=Aussentemperatur, 1=Raumtemperatur, 2= TA/TI kombiniert
	 */
	final static int CONTROLLED_BY = 47; // 0=external temp, 1=room temp, 2= ext./room combined
	/**
	 * Fusspunkt
	 */
	final static int HEATING_CURVE_BASE_POINT = 48;
	/**
	 * Heizkurvenoptimierung?
	 */
	// final static int = 49;
	/**
	 * Steilheit
	 */
	final static int HEATING_CURVE_GRADIENT = 50;
	/**
	 * Heizkurvenoptimierung?
	 */
	// final static int = 51
	/**
	 * Maximale Vorlauf Temperatur
	 */
	final static int MAX_FLOW_TEMP = 52;
	/**
	 * Heizgrenze Heizbetrieb
	 */
	final static int HEATING_LIMIT_TEMP = 53;
	/**
	 * Heizgrenze Absenken
	 */
	final static int HEATING_LIMIT_TEMP_LOWERING = 54;
	/**
	 * Frostschutz Aussentemperatur
	 */
	final static int ANTI_FREEZE_OUTSIDE_TEMP = 55;
	/**
	 * Vorhaltezeit Aufheizen minuten
	 */
	final static int HEAT_UP_TIME = 56; // in minutes
	/**
	 * Raumeinfluss
	 */
	final static int ROOM_IMPACT = 57;
	/**
	 * Ueberhoehung Kessel
	 */
	final static int BOILER_SUPERELEVATION = 58;
	/**
	 * Spreizung Heizkreis
	 */
	final static int SPREADING_HEATING_CIRCUIT = 59;
	/**
	 * Minimale Drehzahl Pumpe PHK %
	 */
	final static int HEATING_MIN_SPEED_PUMP = 60; // in %
	// final static int = 61
	/**
	 * Mischer Laufzeit (minuten)
	 */
	final static int MIXER_RUNTIME = 62; // in minutes
	// final static int = 63
	// final static int = 64
	/**
	 * (Raumtemperatur Abgleich (* 10, neg. Werte sind um 1 zu hoch, 0 und -1 werden
	 * beide als 0 geliefert))
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
	 */
	final static int UNDERFLOOR_HEATING_BASE_POINT = 87;
	// final static int = 88
	/**
	 * (Steilheit Fussbodenheitzung)
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
	 */
	final static int HOT_WATER_TEMP_NORMAL = 149;
	/**
	 * Warmwassertemperatur komfort
	 */
	final static int HOT_WATER_TEMP_COMFORT = 150;
	/**
	 * Warmwasser Normal=1, Komfort=2, Gesperrt=3 ???
	 */
	final static int HOT_WATER_OPERATION_MODE = 151;
	// final static int = 152
	// final static int = 153
	// final static int = 154
	/**
	 * Schaltdifferenz Warmwasser
	 */
	final static int HOT_WATER_HYSTERESIS = 155;
	/**
	 * Maximale Warmwassertemperatur
	 */
	final static int HOT_WATER_TEMP_MAX = 156;
	// final static int = 157
	/**
	 * Nachlauf Pumpe PK/LP
	 */
	final static int PUMP_OVERRUN = 158;
	/**
	 * Maximale Puffer Temperatur
	 */
	final static int BUFFER_TEMP_MAX = 159;
	/**
	 * Minimale Puffer Temperatur
	 */
	final static int BUFFER_TEMP_MIN = 160;
	/**
	 * SchaltDifferenz Kessel
	 */
	final static int BOILER_HYSTERESIS = 161;
	/**
	 * Minimale Laufzeit Kessel (minuten)
	 */
	final static int BOILER_RUNTIME_MIN = 162;
	/**
	 * Abschalt TA Kessel
	 */
	final static int BOILER_SHUTDOWN_TEMP = 163;
	/**
	 * Minimale Drehzahl Pumpe PK %
	 */
	final static int BOILER_MIN_SPEED_PUMP = 164;
	// final static int = 165
	// final static int = 166
	// final static int = 167
	// final static int = 168
	/**
	 * (Nachlaufzeit Pumpe PZ)
	 */
	final static int CIRCULATION_PUMP_OVERRUN = 169;
	// final static int = 170
	/**
	 * (Zirkulation Schaltdifferenz)
	 */
	final static int CIRCULATION_HYSTERESIS = 171;
	// final static int = 172
	// final static int = 173
	// final static int = 174
	// final static int = 175
	/**
	 * Raumtemperatur ändern um
	 */
	final static int ADJUST_ROOM_TEMP_BY = 176;
	// final static int = 177
	// final static int = 178
	/**
	 * Betriebszeit Kessel (Stunden)
	 */
	final static int BOILER_OPERATION_TIME_HOURS = 179;
	/**
	 * Betriebszeit Kessel (Minuten)
	 */
	final static int BOILER_OPERATION_TIME_MINUTES = 180;
	/**
	 * Anzahl Brennerstarts
	 */
	final static int BURNER_NUMBER_STARTS = 181;
	/**
	 * Solare Leistung momentane Leistung der Solaranlage Die solare Leistung
	 * berechnet sich aus folgenden Messwerten: • Differenz zwischen der Temperatur
	 * am Kollektoraustritt und der Temperatur am Kollektoreintritt • Volumenstrom
	 * durch der Solaranlage
	 */
	final static int SOLAR_POWER_ACTUAL = 182;
	/**
	 * Tagesgewinn??? die an diesem Tag bisher von der Solaranlage erzeugte
	 * Energiemenge Die Anzeige wird um Mitternacht selbsttätig auf 0 zurückgesetzt.
	 */
	final static int SOLAR_GAIN_DAY = 183;
	/**
	 * (Solargewinn gesamt???) die insgesamt von der Solaranlage erzeugte
	 * Energiemenge seit Inbetriebnahme der Solaranlage oder seit dem letzten
	 * Löschen des Solargewinns
	 */
	final static int SOLAR_GAIN_TOTAL = 184;
	// final static int = 185
	final static int COUNTDOWN = 186;
	// final static int = 187
	// final static int = 188
	// final static int = 189
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
	 * Relais Heizkreispumpe = Relais & 0x0001 Relais & 0x0002 Relais & 0x0004
	 * Relais & 0x0008 Relais & 0x0010 Ladepumpe = Relais & 0x0080 Zirkulationspumpe
	 * = Relais & 0x0100 Kessel = Relais & 0x0200 Relais & 0x0800 Brenner = Kessel
	 * && (FLOW_TEMP_BOILER - RETURN_TEMP_BOILER > 2)
	 */
	final static int RELAY = 220;
	// final static int = 221
	/**
	 * Status ???
	 */
	// final static int = 222
	// final static int = 223
	// final static int = 224
	// final static int = 225
	// final static int = 226
	/**
	 * ???
	 */
	// final static int = 227
	/**
	 * Fehlerstatus (255 = OK)
	 */
	final static int ERROR = 228;
	// final static int = 229
	/**
	 * Betriebsart ???
	 */
	final static int OPERATION_MODE_X = 230;
	/**
	 * Heizung aus=0; normal=1, komfort=2, abgesenkt=3 ???
	 */
	final static int HEATING_OPERATION_MODE_X = 231;
	/**
	 * Status ???
	 */
	// final static int = 232
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
	 * Ofen pumpe ???
	 */
	final static int STOVE_PUMP_SPEED_ACTUAL = 246;
	// final static int = 247
	/**
	 * Status ???
	 */
	// final static int = 248
	// final static int = 249
}