/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.database.pbmap;

import org.oscim.core.Tag;

public class Tags {
	public final static int MAX = 654;
	public final static int LIMIT = 1024;

	private static final String s_limited = "limited".intern();
	private static final String s_chain = "chain".intern();
	private static final String s_viaduct = "viaduct".intern();
	private static final String s_department_store = "department_store".intern();
	private static final String s_factory = "factory".intern();
	private static final String s_recreation_ground = "recreation_ground".intern();
	private static final String s_nature_reserve = "nature_reserve".intern();
	private static final String s_apartment = "apartment".intern();
	private static final String s_preserved = "preserved".intern();
	private static final String s_stationery = "stationery".intern();
	private static final String s_gravel = "gravel".intern();
	private static final String s_hill = "hill".intern();
	private static final String s_water_well = "water_well".intern();
	private static final String s_garden = "garden".intern();
	private static final String s_permissive = "permissive".intern();
	private static final String s_deli = "deli".intern();
	private static final String s_industrial_retail = "industrial;retail".intern();
	private static final String s_city_wall = "city_wall".intern();
	private static final String s_artwork = "artwork".intern();
	private static final String s_chapel = "chapel".intern();
	private static final String s_school = "school".intern();
	private static final String s_caravan_site = "caravan_site".intern();
	private static final String s_reservoir_watershed = "reservoir_watershed".intern();
	private static final String s_local_authority = "local_authority".intern();
	private static final String s_miniature_golf = "miniature_golf".intern();
	private static final String s_bus_stop = "bus_stop".intern();
	private static final String s_convenience = "convenience".intern();
	private static final String s_kissing_gate = "kissing_gate".intern();
	private static final String s_subway = "subway".intern();
	private static final String s_cutline = "cutline".intern();
	private static final String s_disused = "disused".intern();
	private static final String s_clothes = "clothes".intern();
	private static final String s_bicycle = "bicycle".intern();
	private static final String s_meadow = "meadow".intern();
	private static final String s_fence = "fence".intern();
	private static final String s_video = "video".intern();
	private static final String s_monorail = "monorail".intern();
	private static final String s_clock = "clock".intern();
	private static final String s_dirt = "dirt".intern();
	private static final String s_border_control = "border_control".intern();
	private static final String s_access = "access".intern();
	private static final String s_public = "public".intern();
	private static final String s_fast_food = "fast_food".intern();
	private static final String s_transportation = "transportation".intern();
	private static final String s_commercial = "commercial".intern();
	private static final String s_water = "water".intern();
	private static final String s_beacon = "beacon".intern();
	private static final String s_trunk = "trunk".intern();
	private static final String s_path = "path".intern();
	private static final String s_bicycle_rental = "bicycle_rental".intern();
	private static final String s_miniature = "miniature".intern();
	private static final String s_car_parts = "car_parts".intern();
	private static final String s_light_rail = "light_rail".intern();
	private static final String s_military = "military".intern();
	private static final String s_bog = "bog".intern();
	private static final String s_hiking = "hiking".intern();
	private static final String s_lift_gate = "lift_gate".intern();
	private static final String s_private = "private".intern();
	private static final String s_county = "county".intern();
	private static final String s_secondary_link = "secondary_link".intern();
	private static final String s_marker = "marker".intern();
	private static final String s_islet = "islet".intern();
	private static final String s_holding_position = "holding_position".intern();
	private static final String s_tertiary = "tertiary".intern();
	private static final String s_water_park = "water_park".intern();
	private static final String s_stream = "stream".intern();
	private static final String s_hospital = "hospital".intern();
	private static final String s_destination = "destination".intern();
	private static final String s_MDF = "MDF".intern();
	private static final String s_sports = "sports".intern();
	private static final String s_vineyard = "vineyard".intern();
	private static final String s_music = "music".intern();
	private static final String s_6 = "6".intern();
	private static final String s_entrance = "entrance".intern();
	private static final String s_beauty = "beauty".intern();
	private static final String s_give_way = "give_way".intern();
	private static final String s_kiosk = "kiosk".intern();
	private static final String s_stone = "stone".intern();
	private static final String s_grass_paver = "grass_paver".intern();
	private static final String s_deciduous = "deciduous".intern();
	private static final String s_train = "train".intern();
	private static final String s_organic = "organic".intern();
	private static final String s_farmyard = "farmyard".intern();
	private static final String s_riverbank = "riverbank".intern();
	private static final String s_doityourself = "doityourself".intern();
	private static final String s_town = "town".intern();
	private static final String s_dog_park = "dog_park".intern();
	private static final String s_village_green = "village_green".intern();
	private static final String s_tunnel = "tunnel".intern();
	private static final String s_car = "car".intern();
	private static final String s_roof = "roof".intern();
	private static final String s_mall = "mall".intern();
	private static final String s_ferry_terminal = "ferry_terminal".intern();
	private static final String s_cave_entrance = "cave_entrance".intern();
	private static final String s_detached = "detached".intern();
	private static final String s_concrete_plates = "concrete:plates".intern();
	private static final String s_public_building = "public_building".intern();
	private static final String s_buffer_stop = "buffer_stop".intern();
	private static final String s_lock = "lock".intern();
	private static final String s_dolphin = "dolphin".intern();
	private static final String s_taxiway = "taxiway".intern();
	private static final String s_hunting_stand = "hunting_stand".intern();
	private static final String s_estate_agent = "estate_agent".intern();
	private static final String s_station = "station".intern();
	private static final String s_car_repair = "car_repair".intern();
	private static final String s_dyke = "dyke".intern();
	private static final String s_hangar = "hangar".intern();
	private static final String s_information = "information".intern();
	private static final String s_1 = "1".intern();
	private static final String s_forest = "forest".intern();
	private static final String s_gate = "gate".intern();
	private static final String s_beach = "beach".intern();
	private static final String s_laundry = "laundry".intern();
	private static final String s_speed_camera = "speed_camera".intern();
	private static final String s_staircase = "staircase".intern();
	private static final String s_farm = "farm".intern();
	private static final String s_stop = "stop".intern();
	private static final String s_bump_gate = "bump_gate".intern();
	private static final String s_motorway = "motorway".intern();
	private static final String s_water_tower = "water_tower".intern();
	private static final String s_abutters = "abutters".intern();
	private static final String s_driving_school = "driving_school".intern();
	private static final String s_natural = "natural".intern();
	private static final String s_orchard = "orchard".intern();
	private static final String s_wheelchair = "wheelchair".intern();
	private static final String s_swimming_pool = "swimming_pool".intern();
	private static final String s_switch = "switch".intern();
	private static final String s_block = "block".intern();
	private static final String s_turnstile = "turnstile".intern();
	private static final String s_camp_site = "camp_site".intern();
	private static final String s_shoes = "shoes".intern();
	private static final String s_reservoir = "reservoir".intern();
	private static final String s_pebblestone = "pebblestone".intern();
	private static final String s_stile = "stile".intern();
	private static final String s_embassy = "embassy".intern();
	private static final String s_postal_code = "postal_code".intern();
	private static final String s_retaining_wall = "retaining_wall".intern();
	private static final String s_bridleway = "bridleway".intern();
	private static final String s_pitch = "pitch".intern();
	private static final String s_agricultural = "agricultural".intern();
	private static final String s_post_office = "post_office".intern();
	private static final String s_parking_fuel = "parking;fuel".intern();
	private static final String s_bureau_de_change = "bureau_de_change".intern();
	private static final String s_mini_roundabout = "mini_roundabout".intern();
	private static final String s_hov = "hov".intern();
	private static final String s_police = "police".intern();
	private static final String s_courthouse = "courthouse".intern();
	private static final String s_raceway = "raceway".intern();
	private static final String s_kindergarten = "kindergarten".intern();
	private static final String s_attraction = "attraction".intern();
	private static final String s_marsh = "marsh".intern();
	private static final String s_reservoir_covered = "reservoir_covered".intern();
	private static final String s_petroleum_well = "petroleum_well".intern();
	private static final String s_silo = "silo".intern();
	private static final String s_toys = "toys".intern();
	private static final String s_apron = "apron".intern();
	private static final String s_halt = "halt".intern();
	private static final String s_dam = "dam".intern();
	private static final String s_golf_course = "golf_course".intern();
	private static final String s_detour = "detour".intern();
	private static final String s_tree_row = "tree_row".intern();
	private static final String s_copyshop = "copyshop".intern();
	private static final String s_milestone = "milestone".intern();
	private static final String s_foot = "foot".intern();
	private static final String s_tourism = "tourism".intern();
	private static final String s_bank = "bank".intern();
	private static final String s_dry_cleaning = "dry_cleaning".intern();
	private static final String s_tram = "tram".intern();
	private static final String s_trolleybus = "trolleybus".intern();
	private static final String s_university = "university".intern();
	private static final String s_hampshire_gate = "hampshire_gate".intern();
	private static final String s_embankment = "embankment".intern();
	private static final String s_rock = "rock".intern();
	private static final String s_crossing = "crossing".intern();
	private static final String s_volcano = "volcano".intern();
	private static final String s_greengrocer = "greengrocer".intern();
	private static final String s_kerb = "kerb".intern();
	private static final String s_waste_disposal = "waste_disposal".intern();
	private static final String s_grave_yard = "grave_yard".intern();
	private static final String s_coniferous = "coniferous".intern();
	private static final String s_house = "house".intern();
	private static final String s_books = "books".intern();
	private static final String s_neighbourhood = "neighbourhood".intern();
	private static final String s_hostel = "hostel".intern();
	private static final String s_alcohol = "alcohol".intern();
	private static final String s_restricted = "restricted".intern();
	private static final String s_motel = "motel".intern();
	private static final String s_sand = "sand".intern();
	private static final String s_fishmonger = "fishmonger".intern();
	private static final String s_fountain = "fountain".intern();
	private static final String s_playground = "playground".intern();
	private static final String s_7 = "7".intern();
	private static final String s_parking_aisle = "parking_aisle".intern();
	private static final String s_protected_area = "protected_area".intern();
	private static final String s_electronics = "electronics".intern();
	private static final String s_Paved = "Paved".intern();
	private static final String s_highway = "highway".intern();
	private static final String s_fine_gravel = "fine_gravel".intern();
	private static final String s_barrier = "barrier".intern();
	private static final String s_hairdresser = "hairdresser".intern();
	private static final String s_post_box = "post_box".intern();
	private static final String s_pub = "pub".intern();
	private static final String s_coastline = "coastline".intern();
	private static final String s_marina = "marina".intern();
	private static final String s_reedbed = "reedbed".intern();
	private static final String s_biergarten = "biergarten".intern();
	private static final String s_dismantled = "dismantled".intern();
	private static final String s_farmland = "farmland".intern();
	private static final String s_yard = "yard".intern();
	private static final String s_route = "route".intern();
	private static final String s_atm = "atm".intern();
	private static final String s_place = "place".intern();
	private static final String s_bus_station = "bus_station".intern();
	private static final String s_retail = "retail".intern();
	private static final String s_industrial = "industrial".intern();
	private static final String s_municipality = "municipality".intern();
	private static final String s_primary = "primary".intern();
	private static final String s_nursing_home = "nursing_home".intern();
	private static final String s_florist = "florist".intern();
	private static final String s_ditch = "ditch".intern();
	private static final String s_national_park = "national_park".intern();
	private static final String s_city = "city".intern();
	private static final String s_confectionery = "confectionery".intern();
	private static final String s_service = "service".intern();
	private static final String s_unknown = "unknown".intern();
	private static final String s_cycle_barrier = "cycle_barrier".intern();
	private static final String s_elevator = "elevator".intern();
	private static final String s_2 = "2".intern();
	private static final String s_car_rental = "car_rental".intern();
	private static final String s_flagpole = "flagpole".intern();
	private static final String s_cabin = "cabin".intern();
	private static final String s_paved = "paved".intern();
	private static final String s_guest_house = "guest_house".intern();
	private static final String s_mobile_phone = "mobile_phone".intern();
	private static final String s_lot = "lot".intern();
	private static final String s_quarry = "quarry".intern();
	private static final String s_train_station = "train_station".intern();
	private static final String s_hotel = "hotel".intern();
	private static final String s_park = "park".intern();
	private static final String s_hut = "hut".intern();
	private static final String s_dentist = "dentist".intern();
	private static final String s_doctors = "doctors".intern();
	private static final String s_greenhouse = "greenhouse".intern();
	private static final String s_11 = "11".intern();
	private static final String s_10 = "10".intern();
	private static final String s_theme_park = "theme_park".intern();
	private static final String s_tree = "tree".intern();
	private static final String s_shower = "shower".intern();
	private static final String s_siding = "siding".intern();
	private static final String s_aeroway = "aeroway".intern();
	private static final String s_emergency_access_point = "emergency_access_point"
			.intern();
	private static final String s_watermill = "watermill".intern();
	private static final String s_college = "college".intern();
	private static final String s_landuse = "landuse".intern();
	private static final String s_tracktype = "tracktype".intern();
	private static final String s_ferry = "ferry".intern();
	private static final String s_bridge = "bridge".intern();
	private static final String s_vacant = "vacant".intern();
	private static final String s_cattle_grid = "cattle_grid".intern();
	private static final String s_brownfield = "brownfield".intern();
	private static final String s_allotments = "allotments".intern();
	private static final String s_alley = "alley".intern();
	private static final String s_pedestrian = "pedestrian".intern();
	private static final String s_borough = "borough".intern();
	private static final String s_bare_rock = "bare_rock".intern();
	private static final String s_motorcycle = "motorcycle".intern();
	private static final String s_bakery = "bakery".intern();
	private static final String s_zoo = "zoo".intern();
	private static final String s_scree = "scree".intern();
	private static final String s_fire_station = "fire_station".intern();
	private static final String s_theatre = "theatre".intern();
	private static final String s_track = "track".intern();
	private static final String s_reinforced_slope = "reinforced_slope".intern();
	private static final String s_slipway = "slipway".intern();
	private static final String s_mangrove = "mangrove".intern();
	private static final String s_aerodrome = "aerodrome".intern();
	private static final String s_byway = "byway".intern();
	private static final String s_metal = "metal".intern();
	private static final String s_swamp = "swamp".intern();
	private static final String s_construction = "construction".intern();
	private static final String s_grassland = "grassland".intern();
	private static final String s_shop = "shop".intern();
	private static final String s_soakhole = "soakhole".intern();
	private static final String s_asphalt = "asphalt".intern();
	private static final String s_social_facility = "social_facility".intern();
	private static final String s_isolated_dwelling = "isolated_dwelling".intern();
	private static final String s_hamlet = "hamlet".intern();
	private static final String s_picnic_table = "picnic_table".intern();
	private static final String s_artificial = "artificial".intern();
	private static final String s_earth = "earth".intern();
	private static final String s_grit_bin = "grit_bin".intern();
	private static final String s_ground = "ground".intern();
	private static final String s_groyne = "groyne".intern();
	private static final String s_office = "office".intern();
	private static final String s_state = "state".intern();
	private static final String s_terminal = "terminal".intern();
	private static final String s_wood = "wood".intern();
	private static final String s_fuel = "fuel".intern();
	private static final String s_8 = "8".intern();
	private static final String s_garden_centre = "garden_centre".intern();
	private static final String s_horse_riding = "horse_riding".intern();
	private static final String s_viewpoint = "viewpoint".intern();
	private static final String s_designated = "designated".intern();
	private static final String s_leisure = "leisure".intern();
	private static final String s_waste_basket = "waste_basket".intern();
	private static final String s_hifi = "hifi".intern();
	private static final String s_hedge = "hedge".intern();
	private static final String s_spur = "spur".intern();
	private static final String s_chimney = "chimney".intern();
	private static final String s_secondary = "secondary".intern();
	private static final String s_rest_area = "rest_area".intern();
	private static final String s_bar = "bar".intern();
	private static final String s_bay = "bay".intern();
	private static final String s_common = "common".intern();
	private static final String s_river = "river".intern();
	private static final String s_ruins = "ruins".intern();
	private static final String s_terrace = "terrace".intern();
	private static final String s_art = "art".intern();
	private static final String s_residental = "residental".intern();
	private static final String s_newsagent = "newsagent".intern();
	private static final String s_turntable = "turntable".intern();
	private static final String s_computer = "computer".intern();
	private static final String s_wetland = "wetland".intern();
	private static final String s_driveway = "driveway".intern();
	private static final String s_parking = "parking".intern();
	private static final String s_compacted = "compacted".intern();
	private static final String s_barn = "barn".intern();
	private static final String s_alpine_hut = "alpine_hut".intern();
	private static final String s_wire_fence = "wire_fence".intern();
	private static final String s_unpaved = "unpaved".intern();
	private static final String s_dormitory = "dormitory".intern();
	private static final String s_mud = "mud".intern();
	private static final String s_3 = "3".intern();
	private static final String s_semi = "semi".intern();
	private static final String s_boundary = "boundary".intern();
	private static final String s_field_boundary = "field_boundary".intern();
	private static final String s_beverages = "beverages".intern();
	private static final String s_supermarket = "supermarket".intern();
	private static final String s_store = "store".intern();
	private static final String s_restaurant = "restaurant".intern();
	private static final String s_region = "region".intern();
	private static final String s_variety_store = "variety_store".intern();
	private static final String s_saltmarsh = "saltmarsh".intern();
	private static final String s_landform = "landform".intern();
	private static final String s_helipad = "helipad".intern();
	private static final String s_railway = "railway".intern();
	private static final String s_greenhouse_horticulture = "greenhouse_horticulture"
			.intern();
	private static final String s_wall = "wall".intern();
	private static final String s_recycling = "recycling".intern();
	private static final String s_passing_place = "passing_place".intern();
	private static final String s_church = "church".intern();
	private static final String s_pharmacy = "pharmacy".intern();
	private static final String s_lighthouse = "lighthouse".intern();
	private static final String s_platform = "platform".intern();
	private static final String s_cinema = "cinema".intern();
	private static final String s_political = "political".intern();
	private static final String s_stadium = "stadium".intern();
	private static final String s_basin = "basin".intern();
	private static final String s_gasometer = "gasometer".intern();
	private static final String s_bicycle_parking = "bicycle_parking".intern();
	private static final String s_bbq = "bbq".intern();
	private static final String s_incline_steep = "incline_steep".intern();
	private static final String s_drinking_water = "drinking_water".intern();
	private static final String s_living_street = "living_street".intern();
	private static final String s_chalet = "chalet".intern();
	private static final String s_narrow_gauge = "narrow_gauge".intern();
	private static final String s_prison = "prison".intern();
	private static final String s_mine = "mine".intern();
	private static final String s_level_crossing = "level_crossing".intern();
	private static final String s_water_works = "water_works".intern();
	private static final String s_street_lamp = "street_lamp".intern();
	private static final String s_main = "main".intern();
	private static final String s_tank = "tank".intern();
	private static final String s_abandoned = "abandoned".intern();
	private static final String s_ski = "ski".intern();
	private static final String s_runway = "runway".intern();
	private static final String s_parking_space = "parking_space".intern();
	private static final String s_dirt_sand = "dirt/sand".intern();
	private static final String s_salt_pond = "salt_pond".intern();
	private static final String s_hedge_bank = "hedge_bank".intern();
	private static final String s_amenity = "amenity".intern();
	private static final String s_telephone = "telephone".intern();
	private static final String s_surface = "surface".intern();
	private static final String s_travel_agency = "travel_agency".intern();
	private static final String s_hardware = "hardware".intern();
	private static final String s_wastewater_plant = "wastewater_plant".intern();
	private static final String s_waterway = "waterway".intern();
	private static final String s_butcher = "butcher".intern();
	private static final String s_surveillance = "surveillance".intern();
	private static final String s_Dirt_Sand = "Dirt/Sand".intern();
	private static final String s_9 = "9".intern();
	private static final String s_windmill = "windmill".intern();
	private static final String s_picnic_site = "picnic_site".intern();
	private static final String s_rail = "rail".intern();
	private static final String s_cement = "cement".intern();
	private static final String s_sauna = "sauna".intern();
	private static final String s_suburb = "suburb".intern();
	private static final String s_waterfall = "waterfall".intern();
	private static final String s_bunker = "bunker".intern();
	private static final String s_ice_cream = "ice_cream".intern();
	private static final String s_culvert = "culvert".intern();
	private static final String s_drain = "drain".intern();
	private static final String s_dock = "dock".intern();
	private static final String s_glasshouse = "glasshouse".intern();
	private static final String s_no = "no".intern();
	private static final String s_well = "well".intern();
	private static final String s_wet_meadow = "wet_meadow".intern();
	private static final String s_concrete = "concrete".intern();
	private static final String s_dismount = "dismount".intern();
	private static final String s_vending_machine = "vending_machine".intern();
	private static final String s_oneway = "oneway".intern();
	private static final String s_taxi = "taxi".intern();
	private static final String s_outdoor = "outdoor".intern();
	private static final String s_proposed = "proposed".intern();
	private static final String s_sally_port = "sally_port".intern();
	private static final String s_photo = "photo".intern();
	private static final String s_plant_nursery = "plant_nursery".intern();
	private static final String s_clinic = "clinic".intern();
	private static final String s_fishing = "fishing".intern();
	private static final String s_yes = "yes".intern();
	private static final String s_turning_circle = "turning_circle".intern();
	private static final String s_toilets = "toilets".intern();
	private static final String s_guard_rail = "guard_rail".intern();
	private static final String s_townhall = "townhall".intern();
	private static final String s_community_centre = "community_centre".intern();
	private static final String s_residential = "residential".intern();
	private static final String s_cemetery = "cemetery".intern();
	private static final String s_survey_point = "survey_point".intern();
	private static final String s_bench = "bench".intern();
	private static final String s_4 = "4".intern();
	private static final String s_bollard = "bollard".intern();
	private static final String s_sports_centre = "sports_centre".intern();
	private static final String s_paving_stones_30 = "paving_stones:30".intern();
	private static final String s_administrative = "administrative".intern();
	private static final String s_Building = "Building".intern();
	private static final String s_customers = "customers".intern();
	private static final String s_emergency = "emergency".intern();
	private static final String s_motorway_junction = "motorway_junction".intern();
	private static final String s_grade1 = "grade1".intern();
	private static final String s_grade3 = "grade3".intern();
	private static final String s_grade2 = "grade2".intern();
	private static final String s_grade5 = "grade5".intern();
	private static final String s_grade4 = "grade4".intern();
	private static final String s_lock_gate = "lock_gate".intern();
	private static final String s_furniture = "furniture".intern();
	private static final String s_place_of_worship = "place_of_worship".intern();
	private static final String s_optician = "optician".intern();
	private static final String s_gift = "gift".intern();
	private static final String s_parking_entrance = "parking_entrance".intern();
	private static final String s_garage = "garage".intern();
	private static final String s_tram_stop = "tram_stop".intern();
	private static final String s_steps = "steps".intern();
	private static final String s_tower = "tower".intern();
	private static final String s_works = "works".intern();
	private static final String s_shed = "shed".intern();
	private static final String s_car_sharing = "car_sharing".intern();
	private static final String s_apartments = "apartments".intern();
	private static final String s_spring = "spring".intern();
	private static final String s_village = "village".intern();
	private static final String s_library = "library".intern();
	private static final String s_emergency_access = "emergency_access".intern();
	private static final String s_home = "home".intern();
	private static final String s_farm_auxiliary = "farm_auxiliary".intern();
	private static final String s_primary_link = "primary_link".intern();
	private static final String s_toll_booth = "toll_booth".intern();
	private static final String s_jewelry = "jewelry".intern();
	private static final String s_pet = "pet".intern();
	private static final String s_veterinary = "veterinary".intern();
	private static final String s_man_made = "man_made".intern();
	private static final String s_motorway_link = "motorway_link".intern();
	private static final String s_offices = "offices".intern();
	private static final String s_power = "power".intern();
	private static final String s_weir = "weir".intern();
	private static final String s_unsurfaced = "unsurfaced".intern();
	private static final String s_tertiary_link = "tertiary_link".intern();
	private static final String s_trunk_link = "trunk_link".intern();
	private static final String s_tyres = "tyres".intern();
	private static final String s_paving_stones = "paving_stones".intern();
	private static final String s_pipeline = "pipeline".intern();
	private static final String s_census = "census".intern();
	private static final String s_incline = "incline".intern();
	private static final String s_footway = "footway".intern();
	private static final String s_drive_through = "drive-through".intern();
	private static final String s_island = "island".intern();
	private static final String s_monitoring_station = "monitoring_station".intern();
	private static final String s_nightclub = "nightclub".intern();
	private static final String s_unclassified = "unclassified".intern();
	private static final String s_aquaculture = "aquaculture".intern();
	private static final String s_mixed = "mixed".intern();
	private static final String s_road = "road".intern();
	private static final String s_greenfield = "greenfield".intern();
	private static final String s_breakwater = "breakwater".intern();
	private static final String s_services = "services".intern();
	private static final String s_railway_crossing = "railway_crossing".intern();
	private static final String s_residentiel1 = "residentiel1".intern();
	private static final String s_canal = "canal".intern();
	private static final String s__1 = "-1".intern();
	private static final String s_ridge = "ridge".intern();
	private static final String s_fabric = "fabric".intern();
	private static final String s_museum = "museum".intern();
	private static final String s_communications_tower = "communications_tower".intern();
	private static final String s_semi_detached = "semi-detached".intern();
	private static final String s_conservation = "conservation".intern();
	private static final String s_way = "way".intern();
	private static final String s_wood_fence = "wood_fence".intern();
	private static final String s_manufacture = "manufacture".intern();
	private static final String s_admin_level = "admin_level".intern();
	private static final String s_building_concrete = "building_concrete".intern();
	private static final String s_bus = "bus".intern();
	private static final String s_collapsed = "collapsed".intern();
	private static final String s_ford = "ford".intern();
	private static final String s_delivery = "delivery".intern();
	private static final String s_garages = "garages".intern();
	private static final String s_funeral_directors = "funeral_directors".intern();
	private static final String s_land = "land".intern();
	private static final String s_interlock = "interlock".intern();
	private static final String s_reef = "reef".intern();
	private static final String s_crane = "crane".intern();
	private static final String s_true = "true".intern();
	private static final String s_storage_tank = "storage_tank".intern();
	private static final String s_official = "official".intern();
	private static final String s_subway_entrance = "subway_entrance".intern();
	private static final String s_mtb = "mtb".intern();
	private static final String s_grass = "grass".intern();
	private static final String s_marketplace = "marketplace".intern();
	private static final String s_rapids = "rapids".intern();
	private static final String s_car_wash = "car_wash".intern();
	private static final String s_general = "general".intern();
	private static final String s_cafe = "cafe".intern();
	private static final String s_locality = "locality".intern();
	private static final String s_glacier = "glacier".intern();
	private static final String s_storage = "storage".intern();
	private static final String s_cycleway = "cycleway".intern();
	private static final String s_forestry = "forestry".intern();
	private static final String s_field = "field".intern();
	private static final String s_5 = "5".intern();
	private static final String s_arts_centre = "arts_centre".intern();
	private static final String s_warehouse = "warehouse".intern();
	private static final String s_chemist = "chemist".intern();
	private static final String s_pier = "pier".intern();
	private static final String s_scrub = "scrub".intern();
	private static final String s_shelter = "shelter".intern();
	private static final String s_emergency_phone = "emergency_phone".intern();
	private static final String s_tidalflat = "tidalflat".intern();
	private static final String s_cobblestone = "cobblestone".intern();
	private static final String s_fell = "fell".intern();
	private static final String s_peak = "peak".intern();
	private static final String s_charging_station = "charging_station".intern();
	private static final String s_cliff = "cliff".intern();
	private static final String s_building = "building".intern();
	private static final String s_fire_hydrant = "fire_hydrant".intern();
	private static final String s_traffic_signals = "traffic_signals".intern();
	private static final String s_heath = "heath".intern();
	private static final String s_landfill = "landfill".intern();
	private static final String s_mast = "mast".intern();
	private static final String s_boutique = "boutique".intern();
	private static final String s_boat_storage = "boat_storage".intern();

	public final static Tag[] tags = {

			new Tag(s_building, s_yes, true), new Tag(s_highway, s_residential, true),
			new Tag(s_highway, s_service, true), new Tag(s_waterway, s_stream, true),
			new Tag(s_highway, s_unclassified, true), new Tag(s_highway, s_track, true),
			new Tag(s_oneway, s_yes, true), new Tag(s_natural, s_water, true),
			new Tag(s_highway, s_footway, true), new Tag(s_access, s_private, true),
			new Tag(s_highway, s_tertiary, true), new Tag(s_highway, s_path, true),
			new Tag(s_highway, s_secondary, true), new Tag(s_landuse, s_forest, true),
			new Tag(s_bridge, s_yes, true), new Tag(s_natural, s_tree, true),
			new Tag(s_surface, s_paved, true), new Tag(s_natural, s_wood, true),
			new Tag(s_highway, s_primary, true), new Tag(s_landuse, s_grass, true),
			new Tag(s_landuse, s_residential, true), new Tag(s_surface, s_unpaved, true),
			new Tag(s_highway, s_bus_stop, true), new Tag(s_surface, s_asphalt, true),
			new Tag(s_bicycle, s_yes, true), new Tag(s_amenity, s_parking, true),
			new Tag(s_place, s_locality, true), new Tag(s_railway, s_rail, true),
			new Tag(s_service, s_parking_aisle, true),
			new Tag(s_boundary, s_administrative, true),
			new Tag(s_building, s_house, true), new Tag(s_place, s_village, true),
			new Tag(s_natural, s_coastline, true), new Tag(s_tracktype, s_grade2, true),
			new Tag(s_oneway, s_no, true), new Tag(s_service, s_driveway, true),
			new Tag(s_highway, s_turning_circle, true), new Tag(s_place, s_hamlet, true),
			new Tag(s_natural, s_wetland, true), new Tag(s_tracktype, s_grade3, true),
			new Tag(s_waterway, s_river, true), new Tag(s_highway, s_cycleway, true),
			new Tag(s_barrier, s_fence, true), new Tag(s_building, s_residential, true),
			new Tag(s_amenity, s_school, true), new Tag(s_highway, s_crossing, true),
			new Tag(s_admin_level, s_8, true), new Tag(s_highway, s_trunk, true),
			new Tag(s_amenity, s_place_of_worship, true),
			new Tag(s_landuse, s_farmland, true), new Tag(s_tracktype, s_grade1, true),
			new Tag(s_highway, s_road, true), new Tag(s_landuse, s_farm, true),
			new Tag(s_surface, s_gravel, true), new Tag(s_landuse, s_meadow, true),
			new Tag(s_highway, s_motorway, true),
			new Tag(s_highway, s_traffic_signals, true),
			new Tag(s_building, s_hut, true), new Tag(s_highway, s_motorway_link, true),
			new Tag(s_tracktype, s_grade4, true), new Tag(s_barrier, s_gate, true),
			new Tag(s_highway, s_living_street, true), new Tag(s_bicycle, s_no, true),
			new Tag(s_leisure, s_pitch, true), new Tag(s_tunnel, s_yes, true),
			new Tag(s_surface, s_ground, true), new Tag(s_highway, s_steps, true),
			new Tag(s_natural, s_land, true), new Tag(s_man_made, s_survey_point, true),
			new Tag(s_tracktype, s_grade5, true), new Tag(s_waterway, s_ditch, true),
			new Tag(s_leisure, s_park, true), new Tag(s_amenity, s_restaurant, true),
			new Tag(s_barrier, s_wall, true), new Tag(s_waterway, s_riverbank, true),
			new Tag(s_amenity, s_bench, true), new Tag(s_building, s_garage, true),
			new Tag(s_natural, s_scrub, true), new Tag(s_highway, s_pedestrian, true),
			new Tag(s_natural, s_peak, true), new Tag(s_building, s_entrance, true),
			new Tag(s_landuse, s_reservoir, true), new Tag(s_access, s_yes, true),
			new Tag(s_bicycle, s_designated, true),
			new Tag(s_leisure, s_swimming_pool, true),
			new Tag(s_landuse, s_farmyard, true),
			new Tag(s_railway, s_level_crossing, true),
			new Tag(s_building, s_apartments, true), new Tag(s_surface, s_grass, true),
			new Tag(s_wheelchair, s_yes, true), new Tag(s_service, s_alley, true),
			new Tag(s_landuse, s_industrial, true), new Tag(s_amenity, s_fuel, true),
			new Tag(s_surface, s_dirt, true), new Tag(s_highway, s_trunk_link, true),
			new Tag(s_waterway, s_drain, true), new Tag(s_barrier, s_hedge, true),
			new Tag(s_amenity, s_grave_yard, true),
			new Tag(s_tourism, s_information, true),
			new Tag(s_shop, s_supermarket, true),
			new Tag(s_highway, s_primary_link, true), new Tag(s_wood, s_deciduous, true),
			new Tag(s_leisure, s_playground, true), new Tag(s_building, s_roof, true),
			new Tag(s_building, s_industrial, true),
			new Tag(s_amenity, s_post_box, true), new Tag(s_waterway, s_canal, true),
			new Tag(s_barrier, s_bollard, true), new Tag(s_leisure, s_garden, true),
			new Tag(s_wood, s_mixed, true), new Tag(s_landuse, s_cemetery, true),
			new Tag(s_landuse, s_orchard, true), new Tag(s_shop, s_convenience, true),
			new Tag(s_access, s_permissive, true), new Tag(s_surface, s_concrete, true),
			new Tag(s_surface, s_paving_stones, true), new Tag(s_service, s_spur, true),
			new Tag(s_building, s_garages, true), new Tag(s_amenity, s_bank, true),
			new Tag(s_tourism, s_hotel, true), new Tag(s_access, s_no, true),
			new Tag(s_amenity, s_fast_food, true), new Tag(s_man_made, s_pier, true),
			new Tag(s_amenity, s_kindergarten, true),
			new Tag(s_access, s_agricultural, true),
			new Tag(s_surface, s_cobblestone, true), new Tag(s_wheelchair, s_no, true),
			new Tag(s_amenity, s_cafe, true), new Tag(s_amenity, s_hospital, true),
			new Tag(s_amenity, s_post_office, true),
			new Tag(s_amenity, s_public_building, true),
			new Tag(s_amenity, s_recycling, true),
			new Tag(s_highway, s_street_lamp, true), new Tag(s_man_made, s_tower, true),
			new Tag(s_waterway, s_dam, true), new Tag(s_amenity, s_pub, true),
			new Tag(s_wood, s_coniferous, true), new Tag(s_access, s_destination, true),
			new Tag(s_admin_level, s_6, true), new Tag(s_landuse, s_commercial, true),
			new Tag(s_amenity, s_pharmacy, true), new Tag(s_railway, s_abandoned, true),
			new Tag(s_service, s_yard, true), new Tag(s_place, s_island, true),
			new Tag(s_oneway, s__1, true), new Tag(s_landuse, s_quarry, true),
			new Tag(s_landuse, s_vineyard, true),
			new Tag(s_highway, s_motorway_junction, true),
			new Tag(s_railway, s_station, true), new Tag(s_landuse, s_allotments, true),
			new Tag(s_barrier, s_lift_gate, true), new Tag(s_admin_level, s_10, true),
			new Tag(s_amenity, s_telephone, true), new Tag(s_place, s_town, true),
			new Tag(s_man_made, s_cutline, true), new Tag(s_place, s_suburb, true),
			new Tag(s_aeroway, s_taxiway, true), new Tag(s_wheelchair, s_limited, true),
			new Tag(s_highway, s_secondary_link, true),
			new Tag(s_leisure, s_sports_centre, true),
			new Tag(s_amenity, s_bicycle_parking, true),
			new Tag(s_surface, s_sand, true), new Tag(s_highway, s_stop, true),
			new Tag(s_man_made, s_works, true), new Tag(s_landuse, s_retail, true),
			new Tag(s_amenity, s_fire_station, true), new Tag(s_service, s_siding, true),
			new Tag(s_amenity, s_toilets, true), new Tag(s_bench, s_yes, true),
			new Tag(s_oneway, s_1, true), new Tag(s_surface, s_compacted, true),
			new Tag(s_landuse, s_basin, true), new Tag(s_amenity, s_police, true),
			new Tag(s_railway, s_tram, true), new Tag(s_route, s_road, true),
			new Tag(s_natural, s_cliff, true), new Tag(s_highway, s_construction, true),
			new Tag(s_aeroway, s_aerodrome, true), new Tag(s_entrance, s_yes, true),
			new Tag(s_man_made, s_storage_tank, true), new Tag(s_amenity, s_atm, true),
			new Tag(s_tourism, s_attraction, true), new Tag(s_route, s_bus, true),
			new Tag(s_shop, s_bakery, true), new Tag(s_tourism, s_viewpoint, true),
			new Tag(s_amenity, s_swimming_pool, true), new Tag(s_natural, s_beach, true),
			new Tag(s_tourism, s_picnic_site, true), new Tag(s_oneway, s_true, true),
			new Tag(s_highway, s_bridleway, true), new Tag(s_tourism, s_camp_site, true),
			new Tag(s_abutters, s_residential, true),
			new Tag(s_leisure, s_nature_reserve, true),
			new Tag(s_amenity, s_drinking_water, true), new Tag(s_shop, s_clothes, true),
			new Tag(s_natural, s_heath, true),
			new Tag(s_highway, s_mini_roundabout, true),
			new Tag(s_landuse, s_construction, true),
			new Tag(s_amenity, s_waste_basket, true),
			new Tag(s_railway, s_platform, true), new Tag(s_amenity, s_townhall, true),
			new Tag(s_shop, s_hairdresser, true), new Tag(s_amenity, s_shelter, true),
			new Tag(s_admin_level, s_9, true),
			new Tag(s_building, s_farm_auxiliary, true),
			new Tag(s_amenity, s_library, true), new Tag(s_building, s_detached, true),
			new Tag(s_admin_level, s_4, true), new Tag(s_landuse, s_village_green, true),
			new Tag(s_barrier, s_stile, true), new Tag(s_landuse, s_garages, true),
			new Tag(s_amenity, s_bar, true), new Tag(s_railway, s_buffer_stop, true),
			new Tag(s_wetland, s_marsh, true), new Tag(s_tourism, s_museum, true),
			new Tag(s_barrier, s_cycle_barrier, true), new Tag(s_route, s_bicycle, true),
			new Tag(s_railway, s_tram_stop, true),
			new Tag(s_amenity, s_parking_space, true),
			new Tag(s_barrier, s_retaining_wall, true),
			new Tag(s_landuse, s_recreation_ground, true),
			new Tag(s_amenity, s_university, true),
			new Tag(s_highway, s_tertiary_link, true),
			new Tag(s_building, s_terrace, true), new Tag(s_shop, s_car_repair, true),
			new Tag(s_amenity, s_hunting_stand, true),
			new Tag(s_amenity, s_fountain, true), new Tag(s_man_made, s_pipeline, true),
			new Tag(s_wetland, s_swamp, true), new Tag(s_shop, s_car, true),
			new Tag(s_bench, s_no, true), new Tag(s_tunnel, s_culvert, true),
			new Tag(s_building, s_school, true), new Tag(s_barrier, s_entrance, true),
			new Tag(s_railway, s_disused, true), new Tag(s_railway, s_crossing, true),
			new Tag(s_building, s_church, true),
			new Tag(s_amenity, s_social_facility, true), new Tag(s_natural, s_bay, true),
			new Tag(s_shop, s_kiosk, true), new Tag(s_amenity, s_vending_machine, true),
			new Tag(s_route, s_hiking, true), new Tag(s_natural, s_spring, true),
			new Tag(s_leisure, s_common, true), new Tag(s_railway, s_switch, true),
			new Tag(s_waterway, s_rapids, true), new Tag(s_admin_level, s_7, true),
			new Tag(s_leisure, s_stadium, true), new Tag(s_leisure, s_track, true),
			new Tag(s_place, s_isolated_dwelling, true), new Tag(s_place, s_islet, true),
			new Tag(s_waterway, s_weir, true), new Tag(s_amenity, s_doctors, true),
			new Tag(s_access, s_designated, true),
			new Tag(s_landuse, s_conservation, true),
			new Tag(s_waterway, s_artificial, true),
			new Tag(s_amenity, s_bus_station, true),
			new Tag(s_leisure, s_golf_course, true),
			new Tag(s_shop, s_doityourself, true), new Tag(s_building, s_service, true),
			new Tag(s_tourism, s_guest_house, true), new Tag(s_aeroway, s_runway, true),
			new Tag(s_place, s_city, true), new Tag(s_railway, s_subway, true),
			new Tag(s_man_made, s_wastewater_plant, true),
			new Tag(s_building, s_commercial, true), new Tag(s_railway, s_halt, true),
			new Tag(s_amenity, s_emergency_phone, true),
			new Tag(s_building, s_retail, true), new Tag(s_barrier, s_block, true),
			new Tag(s_leisure, s_recreation_ground, true),
			new Tag(s_access, s_forestry, true), new Tag(s_amenity, s_college, true),
			new Tag(s_highway, s_platform, true), new Tag(s_access, s_unknown, true),
			new Tag(s_man_made, s_water_tower, true),
			new Tag(s_surface, s_pebblestone, true), new Tag(s_bridge, s_viaduct, true),
			new Tag(s_shop, s_butcher, true), new Tag(s_shop, s_florist, true),
			new Tag(s_boundary, s_landuse, true), new Tag(s_aeroway, s_helipad, true),
			new Tag(s_building, s_hangar, true), new Tag(s_natural, s_glacier, true),
			new Tag(s_highway, s_proposed, true), new Tag(s_shop, s_mall, true),
			new Tag(s_barrier, s_toll_booth, true),
			new Tag(s_amenity, s_fire_hydrant, true),
			new Tag(s_building, s_manufacture, true), new Tag(s_building, s_farm, true),
			new Tag(s_surface, s_wood, true), new Tag(s_amenity, s_car_wash, true),
			new Tag(s_amenity, s_dentist, true), new Tag(s_natural, s_marsh, true),
			new Tag(s_man_made, s_surveillance, true), new Tag(s_shop, s_bicycle, true),
			new Tag(s_route, s_foot, true), new Tag(s_amenity, s_theatre, true),
			new Tag(s_building, s_office, true), new Tag(s_railway, s_light_rail, true),
			new Tag(s_man_made, s_petroleum_well, true),
			new Tag(s_amenity, s_taxi, true), new Tag(s_building, s_greenhouse, true),
			new Tag(s_landuse, s_brownfield, true),
			new Tag(s_bicycle, s_permissive, true), new Tag(s_admin_level, s_2, true),
			new Tag(s_aeroway, s_apron, true), new Tag(s_building, s_cabin, true),
			new Tag(s_amenity, s_cinema, true), new Tag(s_access, s_customers, true),
			new Tag(s_tourism, s_motel, true), new Tag(s_railway, s_narrow_gauge, true),
			new Tag(s_amenity, s_marketplace, true), new Tag(s_shop, s_furniture, true),
			new Tag(s_entrance, s_staircase, true), new Tag(s_tourism, s_artwork, true),
			new Tag(s_natural, s_grassland, true), new Tag(s_shop, s_books, true),
			new Tag(s_admin_level, s_5, true), new Tag(s_man_made, s_groyne, true),
			new Tag(s_waterway, s_lock_gate, true),
			new Tag(s_highway, s_emergency_access_point, true),
			new Tag(s_natural, s_sand, true), new Tag(s_landuse, s_military, true),
			new Tag(s_boundary, s_protected_area, true),
			new Tag(s_amenity, s_community_centre, true),
			new Tag(s_barrier, s_kissing_gate, true),
			new Tag(s_highway, s_speed_camera, true),
			new Tag(s_boundary, s_national_park, true),
			new Tag(s_railway, s_subway_entrance, true),
			new Tag(s_man_made, s_silo, true), new Tag(s_shop, s_alcohol, true),
			new Tag(s_highway, s_give_way, true), new Tag(s_leisure, s_slipway, true),
			new Tag(s_shop, s_electronics, true), new Tag(s_bicycle, s_dismount, true),
			new Tag(s_leisure, s_marina, true), new Tag(s_entrance, s_main, true),
			new Tag(s_boundary, s_postal_code, true),
			new Tag(s_landuse, s_greenhouse_horticulture, true),
			new Tag(s_highway, s_milestone, true),
			new Tag(s_natural, s_cave_entrance, true),
			new Tag(s_landuse, s_landfill, true), new Tag(s_shop, s_chemist, true),
			new Tag(s_shop, s_shoes, true), new Tag(s_barrier, s_cattle_grid, true),
			new Tag(s_landuse, s_railway, true), new Tag(s_tourism, s_hostel, true),
			new Tag(s_tourism, s_chalet, true), new Tag(s_place, s_county, true),
			new Tag(s_shop, s_department_store, true), new Tag(s_highway, s_ford, true),
			new Tag(s_natural, s_scree, true), new Tag(s_landuse, s_greenfield, true),
			new Tag(s_amenity, s_nursing_home, true),
			new Tag(s_barrier, s_wire_fence, true),
			new Tag(s_access, s_restricted, true),
			new Tag(s_man_made, s_reservoir_covered, true),
			new Tag(s_amenity, s_bicycle_rental, true), new Tag(s_man_made, s_MDF, true),
			new Tag(s_man_made, s_water_well, true), new Tag(s_landuse, s_field, true),
			new Tag(s_landuse, s_wood, true), new Tag(s_shop, s_hardware, true),
			new Tag(s_tourism, s_alpine_hut, true), new Tag(s_natural, s_tree_row, true),
			new Tag(s_tourism, s_caravan_site, true), new Tag(s_bridge, s_no, true),
			new Tag(s_wetland, s_bog, true), new Tag(s_amenity, s_courthouse, true),
			new Tag(s_route, s_ferry, true), new Tag(s_barrier, s_city_wall, true),
			new Tag(s_amenity, s_veterinary, true), new Tag(s_shop, s_jewelry, true),
			new Tag(s_building, s_transportation, true),
			new Tag(s_amenity, s_arts_centre, true),
			new Tag(s_bicycle, s_official, true), new Tag(s_shop, s_optician, true),
			new Tag(s_shop, s_yes, true), new Tag(s_building, s_collapsed, true),
			new Tag(s_shop, s_garden_centre, true), new Tag(s_man_made, s_chimney, true),
			new Tag(s_man_made, s_mine, true), new Tag(s_bench, s_unknown, true),
			new Tag(s_railway, s_preserved, true), new Tag(s_building, s_public, true),
			new Tag(s_amenity, s_ferry_terminal, true),
			new Tag(s_highway, s_raceway, true), new Tag(s_natural, s_rock, true),
			new Tag(s_tunnel, s_no, true), new Tag(s_building, s_university, true),
			new Tag(s_shop, s_beverages, true),
			new Tag(s_amenity, s_waste_disposal, true),
			new Tag(s_building, s_warehouse, true),
			new Tag(s_leisure, s_water_park, true), new Tag(s_shop, s_gift, true),
			new Tag(s_place, s_farm, true), new Tag(s_wetland, s_tidalflat, true),
			new Tag(s_waterway, s_waterfall, true), new Tag(s_man_made, s_dolphin, true),
			new Tag(s_service, s_drive_through, true),
			new Tag(s_amenity, s_nightclub, true), new Tag(s_building, s_shed, true),
			new Tag(s_shop, s_greengrocer, true), new Tag(s_natural, s_fell, true),
			new Tag(s_wetland, s_wet_meadow, true), new Tag(s_aeroway, s_gate, true),
			new Tag(s_shop, s_computer, true), new Tag(s_man_made, s_lighthouse, true),
			new Tag(s_wetland, s_reedbed, true), new Tag(s_man_made, s_breakwater, true),
			new Tag(s_surface, s_Dirt_Sand, true), new Tag(s_barrier, s_ditch, true),
			new Tag(s_barrier, s_yes, true), new Tag(s_amenity, s_biergarten, true),
			new Tag(s_shop, s_mobile_phone, true), new Tag(s_route, s_mtb, true),
			new Tag(s_amenity, s_grit_bin, true), new Tag(s_amenity, s_bbq, true),
			new Tag(s_shop, s_sports, true), new Tag(s_barrier, s_wood_fence, true),
			new Tag(s_entrance, s_home, true), new Tag(s_shop, s_laundry, true),
			new Tag(s_man_made, s_gasometer, true),
			new Tag(s_barrier, s_embankment, true), new Tag(s_shop, s_toys, true),
			new Tag(s_wetland, s_saltmarsh, true), new Tag(s_waterway, s_soakhole, true),
			new Tag(s_shop, s_travel_agency, true),
			new Tag(s_man_made, s_water_works, true), new Tag(s_route, s_railway, true),
			new Tag(s_amenity, s_prison, true), new Tag(s_highway, s_rest_area, true),
			new Tag(s_shop, s_stationery, true), new Tag(s_admin_level, s_11, true),
			new Tag(s_building, s_train_station, true),
			new Tag(s_building, s_storage_tank, true),
			new Tag(s_man_made, s_windmill, true), new Tag(s_shop, s_beauty, true),
			new Tag(s_building, s_semi, true), new Tag(s_highway, s_services, true),
			new Tag(s_bicycle, s_private, true), new Tag(s_route, s_ski, true),
			new Tag(s_service, s_emergency_access, true),
			new Tag(s_building, s_factory, true),
			new Tag(s_man_made, s_reinforced_slope, true),
			new Tag(s_amenity, s_car_sharing, true), new Tag(s_surface, s_earth, true),
			new Tag(s_shop, s_hifi, true), new Tag(s_amenity, s_car_rental, true),
			new Tag(s_barrier, s_hedge_bank, true),
			new Tag(s_shop, s_confectionery, true), new Tag(s_aeroway, s_terminal, true),
			new Tag(s_highway, s_passing_place, true),
			new Tag(s_building, s_building, true), new Tag(s_man_made, s_dyke, true),
			new Tag(s_building, s_construction, true), new Tag(s_building, s_shop, true),
			new Tag(s_natural, s_reef, true), new Tag(s_landuse, s_aquaculture, true),
			new Tag(s_shop, s_dry_cleaning, true), new Tag(s_amenity, s_embassy, true),
			new Tag(s_shop, s_newsagent, true), new Tag(s_landuse, s_salt_pond, true),
			new Tag(s_railway, s_spur, true), new Tag(s_wheelchair, s_unknown, true),
			new Tag(s_tourism, s_zoo, true), new Tag(s_man_made, s_waterway, true),
			new Tag(s_surface, s_fine_gravel, true), new Tag(s_shop, s_motorcycle, true),
			new Tag(s_building, s_Building, true),
			new Tag(s_railway, s_construction, true),
			new Tag(s_place, s_neighbourhood, true), new Tag(s_route, s_train, true),
			new Tag(s_building, s_no, true), new Tag(s_natural, s_mud, true),
			new Tag(s_place, s_region, true),
			new Tag(s_landuse, s_reservoir_watershed, true),
			new Tag(s_boundary, s_marker, true), new Tag(s_man_made, s_beacon, true),
			new Tag(s_shop, s_outdoor, true), new Tag(s_access, s_public, true),
			new Tag(s_abutters, s_industrial, true), new Tag(s_building, s_barn, true),
			new Tag(s_leisure, s_picnic_table, true),
			new Tag(s_building, s_hospital, true), new Tag(s_access, s_official, true),
			new Tag(s_shop, s_variety_store, true), new Tag(s_man_made, s_crane, true),
			new Tag(s_amenity, s_parking_fuel, true), new Tag(s_route, s_tram, true),
			new Tag(s_tourism, s_theme_park, true), new Tag(s_shop, s_pet, true),
			new Tag(s_building, s_kindergarten, true),
			new Tag(s_man_made, s_storage, true), new Tag(s_man_made, s_mast, true),
			new Tag(s_amenity, s_parking_entrance, true),
			new Tag(s_amenity, s_clock, true),
			new Tag(s_landuse, s_industrial_retail, true),
			new Tag(s_shop, s_video, true), new Tag(s_access, s_delivery, true),
			new Tag(s_amenity, s_driving_school, true), new Tag(s_service, s_yes, true),
			new Tag(s_natural, s_bare_rock, true), new Tag(s_building, s_chapel, true),
			new Tag(s_natural, s_volcano, true), new Tag(s_waterway, s_dock, true),
			new Tag(s_building, s_dormitory, true),
			new Tag(s_amenity, s_boat_storage, true), new Tag(s_man_made, s_tank, true),
			new Tag(s_man_made, s_flagpole, true),
			new Tag(s_surface, s_grass_paver, true), new Tag(s_shop, s_organic, true),
			new Tag(s_natural, s_landform, true), new Tag(s_highway, s_unsurfaced, true),
			new Tag(s_route, s_power, true), new Tag(s_surface, s_mud, true),
			new Tag(s_building, s_building_concrete, true),
			new Tag(s_abutters, s_retail, true), new Tag(s_building, s_store, true),
			new Tag(s_shop, s_vacant, true), new Tag(s_leisure, s_miniature_golf, true),
			new Tag(s_man_made, s_monitoring_station, true),
			new Tag(s_natural, s_waterfall, true), new Tag(s_aeroway, s_hangar, true),
			new Tag(s_shop, s_boutique, true), new Tag(s_route, s_detour, true),
			new Tag(s_building, s_way, true), new Tag(s_railway, s_stop, true),
			new Tag(s_amenity, s_ice_cream, true), new Tag(s_building, s_storage, true),
			new Tag(s_shop, s_car_parts, true), new Tag(s_natural, s_ridge, true),
			new Tag(s_shop, s_tyres, true), new Tag(s_railway, s_dismantled, true),
			new Tag(s_amenity, s_shop, true), new Tag(s_landuse, s_plant_nursery, true),
			new Tag(s_building, s_residentiel1, true),
			new Tag(s_barrier, s_field_boundary, true),
			new Tag(s_barrier, s_border_control, true),
			new Tag(s_surface, s_Paved, true), new Tag(s_barrier, s_sally_port, true),
			new Tag(s_amenity, s_bureau_de_change, true),
			new Tag(s_leisure, s_fishing, true),
			new Tag(s_amenity, s_charging_station, true),
			new Tag(s_building, s_supermarket, true), new Tag(s_highway, s_stile, true),
			new Tag(s_amenity, s_sauna, true), new Tag(s_place, s_municipality, true),
			new Tag(s_building, s_hotel, true), new Tag(s_surface, s_metal, true),
			new Tag(s_highway, s_incline_steep, true),
			new Tag(s_shop, s_estate_agent, true), new Tag(s_natural, s_grass, true),
			new Tag(s_shop, s_pharmacy, true),
			new Tag(s_surface, s_concrete_plates, true),
			new Tag(s_shop, s_copyshop, true),
			new Tag(s_surface, s_paving_stones_30, true),
			new Tag(s_surface, s_interlock, true), new Tag(s_access, s_hov, true),
			new Tag(s_highway, s_elevator, true),
			new Tag(s_boundary, s_local_authority, true),
			new Tag(s_man_made, s_communications_tower, true),
			new Tag(s_shop, s_deli, true), new Tag(s_barrier, s_turnstile, true),
			new Tag(s_building, s_offices, true), new Tag(s_building, s_bunker, true),
			new Tag(s_natural, s_stone, true),
			new Tag(s_railway, s_railway_crossing, true),
			new Tag(s_leisure, s_dog_park, true),
			new Tag(s_building, s_semi_detached, true),
			new Tag(s_man_made, s_watermill, true), new Tag(s_route, s_trolleybus, true),
			new Tag(s_admin_level, s_3, true), new Tag(s_building, s_block, true),
			new Tag(s_barrier, s_guard_rail, true), new Tag(s_bicycle, s_unknown, true),
			new Tag(s_highway, s_abandoned, true), new Tag(s_surface, s_dirt_sand, true),
			new Tag(s_barrier, s_chain, true), new Tag(s_barrier, s_bump_gate, true),
			new Tag(s_building, s_residental, true), new Tag(s_surface, s_cement, true),
			new Tag(s_man_made, s_embankment, true), new Tag(s_building, s_ruins, true),
			new Tag(s_highway, s_incline, true), new Tag(s_abutters, s_commercial, true),
			new Tag(s_barrier, s_hampshire_gate, true), new Tag(s_shop, s_music, true),
			new Tag(s_shop, s_funeral_directors, true),
			new Tag(s_wetland, s_mangrove, true), new Tag(s_place, s_borough, true),
			new Tag(s_building, s_apartment, true), new Tag(s_boundary, s_census, true),
			new Tag(s_barrier, s_kerb, true), new Tag(s_building, s_glasshouse, true),
			new Tag(s_aeroway, s_holding_position, true),
			new Tag(s_shop, s_general, true), new Tag(s_building, s_tank, true),
			new Tag(s_railway, s_monorail, true), new Tag(s_service, s_parking, true),
			new Tag(s_place, s_state, true), new Tag(s_railway, s_proposed, true),
			new Tag(s_shop, s_art, true), new Tag(s_natural, s_hill, true),
			new Tag(s_railway, s_turntable, true), new Tag(s_tourism, s_cabin, true),
			new Tag(s_shop, s_photo, true), new Tag(s_boundary, s_lot, true),
			new Tag(s_shop, s_fishmonger, true), new Tag(s_amenity, s_clinic, true),
			new Tag(s_boundary, s_political, true), new Tag(s_man_made, s_well, true),
			new Tag(s_highway, s_byway, true), new Tag(s_leisure, s_horse_riding, true),
			new Tag(s_service, s_bus, true), new Tag(s_building, s_tower, true),
			new Tag(s_entrance, s_service, true), new Tag(s_shop, s_fabric, true),
			new Tag(s_railway, s_miniature, true), new Tag(s_abutters, s_mixed, true),
			new Tag(s_surface, s_stone, true), new Tag(s_access, s_emergency, true),
			new Tag(s_landuse, s_mine, true), new Tag(s_amenity, s_shower, true),
			new Tag(s_waterway, s_lock, true)
	};
}
