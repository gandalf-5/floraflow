package com.floraflow.app.data

import android.util.Log
import com.floraflow.app.api.FloraFlowApi
import com.floraflow.app.api.InsightRequest
import com.floraflow.app.api.QuizRequest
import com.floraflow.app.api.UnsplashApi
import com.floraflow.app.api.UnsplashPhoto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PlantRepository(
    private val dao: DailyPlantDao,
    private val unsplashApi: UnsplashApi,
    private val floraFlowApi: FloraFlowApi,
    private val preferredCategories: List<String> = PreferencesManager.ALL_CATEGORIES
) {
    companion object {
        private const val TAG = "PlantRepository"

        /** Lightweight data class for plant specs: Unsplash query, display name, native region. */
        data class PlantSpec(val query: String, val name: String, val region: String)

        /**
         * 406 curated plants for daily discovery — more than 1 full year of unique plants.
         * Ordered to alternate between world regions and plant families for diversity.
         */
        private val DAILY_PLANTS: List<PlantSpec> = listOf(
            // ── Spring Blooms ──────────────────────────────────────────────────────────────
            PlantSpec("Prunus serrulata cherry blossom sakura", "Cherry Blossom", "Japan & Korea"),
            PlantSpec("Tulipa gesneriana tulip flower", "Garden Tulip", "Central Asia & Turkey"),
            PlantSpec("Narcissus pseudonarcissus daffodil", "Daffodil", "Mediterranean Europe"),
            PlantSpec("Magnolia stellata star magnolia", "Star Magnolia", "Japan"),
            PlantSpec("Wisteria sinensis wisteria vine", "Chinese Wisteria", "China"),
            PlantSpec("Syringa vulgaris lilac flower", "Common Lilac", "Balkans"),
            PlantSpec("Paeonia lactiflora peony flower", "Chinese Peony", "China"),
            PlantSpec("Iris germanica bearded iris flower", "Bearded Iris", "Mediterranean"),
            PlantSpec("Forsythia intermedia forsythia yellow flower", "Border Forsythia", "China & Korea"),
            PlantSpec("Hyacinthus orientalis hyacinth flower", "Common Hyacinth", "Eastern Mediterranean"),
            PlantSpec("Anemone coronaria poppy anemone", "Poppy Anemone", "Mediterranean"),
            PlantSpec("Fritillaria imperialis crown imperial lily", "Crown Imperial", "Himalayas to Turkey"),
            PlantSpec("Lamprocapnos spectabilis bleeding heart", "Bleeding Heart", "Siberia & Japan"),
            PlantSpec("Mertensia virginica Virginia bluebells", "Virginia Bluebells", "Eastern North America"),
            PlantSpec("Trillium grandiflorum white trillium", "Great White Trillium", "Eastern North America"),
            PlantSpec("Primula vulgaris primrose flower", "Common Primrose", "Western Europe"),
            PlantSpec("Muscari armeniacum grape hyacinth blue", "Grape Hyacinth", "Turkey & Caucasus"),
            PlantSpec("Camellia japonica japanese camellia flower", "Japanese Camellia", "Japan & China"),
            PlantSpec("Prunus persica peach blossom spring", "Peach Blossom", "Northwest China"),
            PlantSpec("Cercis siliquastrum judas tree flower", "Judas Tree", "Mediterranean"),
            PlantSpec("Crocus vernus spring crocus flower", "Spring Crocus", "Alps & Pyrenees"),
            PlantSpec("Hepatica nobilis liverwort flower", "Hepatica", "Europe & Japan"),
            PlantSpec("Galanthus nivalis snowdrop flower", "Common Snowdrop", "Europe"),
            PlantSpec("Scilla siberica siberian squill blue", "Siberian Squill", "Caucasus"),
            PlantSpec("Cornus florida dogwood blossom spring", "Flowering Dogwood", "Eastern North America"),
            PlantSpec("Amelanchier serviceberry white blossom", "Serviceberry", "North America"),
            PlantSpec("Erythronium dens-canis dog's tooth violet", "Dog's Tooth Violet", "Europe & Asia"),
            PlantSpec("Chionodoxa luciliae glory of the snow", "Glory of the Snow", "Turkey"),
            PlantSpec("Pulmonaria officinalis lungwort flower", "Common Lungwort", "Europe"),
            PlantSpec("Convallaria majalis lily of the valley", "Lily of the Valley", "Europe & Asia"),
            // ── Summer Blooms ──────────────────────────────────────────────────────────────
            PlantSpec("Helianthus annuus sunflower field", "Common Sunflower", "North America"),
            PlantSpec("Lavandula angustifolia english lavender field", "English Lavender", "Western Mediterranean"),
            PlantSpec("Hibiscus rosa-sinensis tropical hibiscus", "Chinese Hibiscus", "East Asia"),
            PlantSpec("Rosa damascena damask rose flower", "Damask Rose", "Middle East"),
            PlantSpec("Nelumbo nucifera sacred lotus pink", "Sacred Lotus", "India & East Asia"),
            PlantSpec("Bougainvillea glabra bougainvillea flower", "Bougainvillea", "Brazil"),
            PlantSpec("Zinnia elegans zinnia flower garden", "Common Zinnia", "Mexico"),
            PlantSpec("Jasminum officinale jasmine white flower", "Common Jasmine", "Persia & North India"),
            PlantSpec("Echinacea purpurea purple coneflower", "Purple Coneflower", "Central North America"),
            PlantSpec("Agapanthus africanus lily of the nile blue", "Lily of the Nile", "South Africa"),
            PlantSpec("Passiflora caerulea passion flower blue", "Blue Passionflower", "South America"),
            PlantSpec("Alstroemeria peruvian lily flower", "Peruvian Lily", "Andes Mountains"),
            PlantSpec("Lathyrus odoratus sweet pea flower", "Sweet Pea", "Sicily & Southern Italy"),
            PlantSpec("Delphinium elatum larkspur blue flower", "Candle Larkspur", "Central Asia"),
            PlantSpec("Lupinus polyphyllus lupin flower blue", "Garden Lupin", "Western North America"),
            PlantSpec("Digitalis purpurea foxglove flower pink", "Common Foxglove", "Western Europe"),
            PlantSpec("Rudbeckia hirta black eyed susan flower", "Black-eyed Susan", "North America"),
            PlantSpec("Monarda didyma bee balm red flower", "Scarlet Bee Balm", "Eastern North America"),
            PlantSpec("Gypsophila paniculata baby's breath flower", "Baby's Breath", "Central Europe"),
            PlantSpec("Phlox paniculata garden phlox flower", "Garden Phlox", "Eastern North America"),
            PlantSpec("Salvia nemorosa sage purple flower", "Balkan Clary", "Europe"),
            PlantSpec("Liatris spicata blazing star purple flower", "Spike Blazing Star", "Eastern North America"),
            PlantSpec("Verbascum thapsus mullein flower yellow", "Great Mullein", "Europe & Asia"),
            PlantSpec("Coreopsis grandiflora tickseed yellow flower", "Large-flowered Tickseed", "North America"),
            PlantSpec("Echinops ritro globe thistle blue flower", "Small Globe Thistle", "Southeastern Europe"),
            PlantSpec("Kniphofia uvaria red hot poker flower", "Red Hot Poker", "South Africa"),
            PlantSpec("Verbena bonariensis tall verbena purple", "Purpletop Vervain", "South America"),
            PlantSpec("Eryngium planum sea holly blue", "Flat Sea Holly", "Central Europe"),
            PlantSpec("Nigella damascena love in a mist flower", "Love-in-a-Mist", "Mediterranean"),
            PlantSpec("Scabiosa atropurpurea pincushion flower purple", "Sweet Scabious", "Southern Europe"),
            // ── Autumn Blooms ─────────────────────────────────────────────────────────────
            PlantSpec("Chrysanthemum morifolium chrysanthemum", "Garden Chrysanthemum", "China & Japan"),
            PlantSpec("Acer palmatum japanese maple autumn leaves", "Japanese Maple", "Japan & Korea"),
            PlantSpec("Aster novi-belgii michaelmas daisy purple", "New York Aster", "Eastern North America"),
            PlantSpec("Hydrangea macrophylla mophead flower", "Bigleaf Hydrangea", "Japan"),
            PlantSpec("Dahlia pinnata dahlia flower red", "Garden Dahlia", "Mexico"),
            PlantSpec("Solidago canadensis goldenrod yellow", "Canada Goldenrod", "North America"),
            PlantSpec("Colchicum autumnale autumn crocus pink", "Meadow Saffron", "Europe"),
            PlantSpec("Helenium autumnale sneezeweed orange", "Common Sneezeweed", "North America"),
            PlantSpec("Gentiana sino-ornata gentian blue flower", "Ornate Gentian", "Western China"),
            PlantSpec("Nerine bowdenii guernsey lily pink", "Guernsey Lily", "South Africa"),
            PlantSpec("Cyclamen hederifolium autumn cyclamen pink", "Ivy-leaved Cyclamen", "Mediterranean"),
            PlantSpec("Tricyrtis hirta toad lily flower", "Hairy Toad Lily", "Japan"),
            PlantSpec("Anemone hybrida japanese anemone white", "Japanese Anemone", "China"),
            PlantSpec("Callicarpa bodinieri beautyberry purple", "Bodinieri's Beautyberry", "China"),
            PlantSpec("Euonymus alatus burning bush red autumn", "Burning Bush", "China, Japan & Korea"),
            PlantSpec("Schizostylis coccinea river lily red", "Crimson Flag Lily", "South Africa"),
            PlantSpec("Kirengeshoma palmata yellow waxbells", "Yellow Waxbells", "Japan & Korea"),
            PlantSpec("Persicaria amplexicaulis mountain fleece red", "Red Bistort", "Himalayas"),
            PlantSpec("Fothergilla gardenii witch alder white", "Dwarf Fothergilla", "Southeastern North America"),
            PlantSpec("Leucothoe fontanesiana drooping leucothoe autumn", "Dog Hobble", "Eastern North America"),
            // ── Winter Blooms ─────────────────────────────────────────────────────────────
            PlantSpec("Euphorbia pulcherrima poinsettia red christmas", "Poinsettia", "Mexico"),
            PlantSpec("Helleborus hybridus lenten rose flower", "Lenten Rose", "Europe"),
            PlantSpec("Hamamelis mollis witch hazel yellow flower", "Chinese Witch Hazel", "China"),
            PlantSpec("Jasminum nudiflorum winter jasmine yellow", "Winter Jasmine", "China"),
            PlantSpec("Chimonanthus praecox wintersweet yellow flower", "Wintersweet", "China"),
            PlantSpec("Daphne mezereum mezereon pink flower", "February Daphne", "Europe"),
            PlantSpec("Ilex aquifolium english holly berries", "English Holly", "Europe"),
            PlantSpec("Mahonia japonica japanese mahonia yellow", "Japanese Mahonia", "China & Taiwan"),
            PlantSpec("Sarcococca hookeriana sweet box flower", "Sweet Box", "China & Himalayas"),
            PlantSpec("Viburnum tinus laurustinus white flower", "Laurustinus", "Mediterranean"),
            // ── Tropical Wonders ──────────────────────────────────────────────────────────
            PlantSpec("Strelitzia reginae bird of paradise flower", "Bird of Paradise", "South Africa"),
            PlantSpec("Heliconia rostrata lobster claw flower", "Lobster Claw Heliconia", "South America"),
            PlantSpec("Anthurium andreanum flamingo lily red", "Flamingo Lily", "Colombia & Ecuador"),
            PlantSpec("Alpinia purpurata red ginger torch flower", "Red Torch Ginger", "Malaysia"),
            PlantSpec("Calathea orbifolia tropical leaf plant", "Calathea", "Bolivia"),
            PlantSpec("Monstera deliciosa swiss cheese plant leaf", "Swiss Cheese Plant", "Southern Mexico"),
            PlantSpec("Ravenala madagascariensis travelers palm", "Traveller's Palm", "Madagascar"),
            PlantSpec("Medinilla magnifica rose grape flower", "Rose Grape", "Philippines"),
            PlantSpec("Gloriosa superba flame lily flower", "Flame Lily", "Tropical Africa & Asia"),
            PlantSpec("Costus speciosus spiral ginger flower", "Spiral Ginger", "Southeast Asia"),
            PlantSpec("Alpinia zerumbet shell ginger flower white", "Shell Ginger", "East Asia"),
            PlantSpec("Hedychium gardnerianum kahili ginger yellow", "Kahili Ginger", "Himalayas"),
            PlantSpec("Strongylodon macrobotrys jade vine green", "Jade Vine", "Philippines"),
            PlantSpec("Tacca chantrieri black bat plant flower", "Black Bat Plant", "Southeast Asia"),
            PlantSpec("Thunbergia grandiflora sky vine blue", "Sky Vine", "India"),
            // ── Orchids ───────────────────────────────────────────────────────────────────
            PlantSpec("Phalaenopsis amabilis moth orchid white", "Moth Orchid", "Southeast Asia"),
            PlantSpec("Cattleya labiata corsage orchid purple", "Corsage Orchid", "Brazil"),
            PlantSpec("Dendrobium nobile noble dendrobium orchid", "Noble Dendrobium", "Himalayas to China"),
            PlantSpec("Vanda coerulea blue vanda orchid", "Blue Vanda Orchid", "Northeast India"),
            PlantSpec("Ophrys apifera bee orchid wild flower", "Bee Orchid", "Europe & Mediterranean"),
            PlantSpec("Dactylorhiza fuchsii common spotted orchid", "Common Spotted Orchid", "Europe"),
            PlantSpec("Paphiopedilum slipper orchid flower", "Lady's Slipper Orchid", "Southeast Asia"),
            PlantSpec("Coelogyne cristata necklace orchid white", "Necklace Orchid", "Himalayas"),
            PlantSpec("Dracula simia monkey face orchid", "Monkey Face Orchid", "Ecuador & Peru"),
            PlantSpec("Vanilla planifolia vanilla orchid flower", "Vanilla Orchid", "Mexico"),
            PlantSpec("Epidendrum ibaguense crucifix orchid red", "Crucifix Orchid", "Colombia"),
            PlantSpec("Zygopetalum orchid blue purple flower", "Zygopetalum Orchid", "South America"),
            PlantSpec("Maxillaria tenuifolia coconut orchid red", "Coconut Orchid", "Mexico to Colombia"),
            PlantSpec("Prosthechea cochleata clamshell orchid", "Clamshell Orchid", "Mexico & Caribbean"),
            PlantSpec("Bulbophyllum lobbii orchid brown", "Lobb's Bulbophyllum", "Southeast Asia"),
            // ── Succulents & Cacti ────────────────────────────────────────────────────────
            PlantSpec("Echeveria elegans mexican snowball succulent", "Mexican Snowball", "Mexico"),
            PlantSpec("Aloe vera medicinal plant green", "Aloe Vera", "Arabian Peninsula"),
            PlantSpec("Agave americana century plant", "Century Plant", "Mexico"),
            PlantSpec("Haworthia attenuata zebra haworthia succulent", "Zebra Haworthia", "South Africa"),
            PlantSpec("Crassula ovata jade plant money tree", "Jade Plant", "South Africa"),
            PlantSpec("Lithops optica living stones succulent", "Living Stone", "Namibia & South Africa"),
            PlantSpec("Carnegiea gigantea saguaro cactus arizona", "Saguaro Cactus", "Sonoran Desert"),
            PlantSpec("Opuntia ficus-indica prickly pear cactus", "Prickly Pear Cactus", "Mexico"),
            PlantSpec("Echinocactus grusonii golden barrel cactus", "Golden Barrel Cactus", "Central Mexico"),
            PlantSpec("Schlumbergera truncata christmas cactus pink", "Christmas Cactus", "Brazil"),
            PlantSpec("Stapelia gigantea starfish cactus flower", "Starfish Cactus", "South Africa"),
            PlantSpec("Dudleya brittonii chalk liveforever succulent", "Chalk Liveforever", "Baja California"),
            PlantSpec("Aeonium arboreum tree aeonium purple", "Tree Aeonium", "Canary Islands"),
            PlantSpec("Kalanchoe blossfeldiana flaming katy red", "Flaming Katy", "Madagascar"),
            PlantSpec("Sempervivum tectorum houseleek succulent", "Common Houseleek", "Mountains of Europe"),
            PlantSpec("Ferocactus cylindraceus california barrel cactus", "California Barrel Cactus", "Sonoran Desert"),
            PlantSpec("Mammillaria hahniana old lady cactus mexico", "Old Lady Cactus", "Mexico"),
            PlantSpec("Cereus repandus columnar cactus", "Peruvian Apple Cactus", "South America"),
            PlantSpec("Aloe ferox cape aloe south africa", "Cape Aloe", "South Africa"),
            PlantSpec("Sedum acre stonecrop succulent yellow", "Biting Stonecrop", "Europe & Asia"),
            // ── Ferns ─────────────────────────────────────────────────────────────────────
            PlantSpec("Cyathea medullaris black tree fern", "Black Tree Fern", "New Zealand"),
            PlantSpec("Osmunda regalis royal fern green", "Royal Fern", "Europe, Asia & Americas"),
            PlantSpec("Asplenium nidus birds nest fern tropical", "Bird's Nest Fern", "Tropical Asia"),
            PlantSpec("Adiantum capillus-veneris maidenhair fern", "Maidenhair Fern", "Worldwide Tropics"),
            PlantSpec("Dryopteris filix-mas male fern forest", "Male Fern", "Northern Hemisphere"),
            PlantSpec("Matteuccia struthiopteris ostrich fern green", "Ostrich Fern", "Northern Hemisphere"),
            PlantSpec("Platycerium bifurcatum staghorn fern", "Staghorn Fern", "Eastern Australia"),
            PlantSpec("Selaginella lepidophylla resurrection plant", "Resurrection Plant", "Chihuahuan Desert"),
            PlantSpec("Dicksonia antarctica soft tree fern australia", "Soft Tree Fern", "Eastern Australia"),
            PlantSpec("Phlebodium aureum blue star fern", "Blue Star Fern", "Tropical Americas"),
            PlantSpec("Nephrolepis exaltata boston fern", "Boston Fern", "Tropical Regions"),
            PlantSpec("Polystichum setiferum soft shield fern", "Soft Shield Fern", "Europe"),
            PlantSpec("Athyrium filix-femina lady fern green", "Lady Fern", "Northern Hemisphere"),
            PlantSpec("Blechnum spicant hard fern forest", "Hard Fern", "Northern Hemisphere"),
            PlantSpec("Polypodium vulgare common polypody fern", "Common Polypody", "Europe"),
            // ── Aquatic Plants ────────────────────────────────────────────────────────────
            PlantSpec("Nymphaea alba white water lily pond", "White Water Lily", "Europe & North Africa"),
            PlantSpec("Nuphar lutea yellow water lily river", "Yellow Water Lily", "Europe & Western Asia"),
            PlantSpec("Nymphaea caerulea blue lotus egypt", "Blue Lotus", "Egypt & East Africa"),
            PlantSpec("Victoria amazonica giant water lily amazon", "Giant Amazon Water Lily", "Amazon Basin"),
            PlantSpec("Eichhornia crassipes water hyacinth purple", "Water Hyacinth", "Amazon Basin"),
            PlantSpec("Pontederia cordata pickerelweed purple", "Pickerelweed", "North & South America"),
            PlantSpec("Cyperus papyrus egyptian papyrus plant", "Egyptian Papyrus", "North Africa"),
            PlantSpec("Typha latifolia cattail marsh", "Broadleaf Cattail", "Worldwide"),
            PlantSpec("Iris pseudacorus yellow flag iris water", "Yellow Flag Iris", "Europe & Asia"),
            PlantSpec("Caltha palustris marsh marigold yellow", "Marsh Marigold", "Northern Hemisphere"),
            PlantSpec("Aponogeton distachyos water hawthorn flower", "Water Hawthorn", "South Africa"),
            PlantSpec("Nymphoides peltata fringed water lily yellow", "Yellow Floating Heart", "Europe & Asia"),
            PlantSpec("Nelumbo lutea american lotus yellow", "American Lotus", "North America"),
            PlantSpec("Sagittaria latifolia arrowhead plant water", "Broadleaf Arrowhead", "North America"),
            PlantSpec("Butomus umbellatus flowering rush pink", "Flowering Rush", "Europe & Asia"),
            // ── Trees & Bonsai ────────────────────────────────────────────────────────────
            PlantSpec("Juniperus procumbens nana bonsai tree", "Juniper Bonsai", "Japan"),
            PlantSpec("Ficus retusa ginseng bonsai", "Ginseng Ficus Bonsai", "Southeast Asia"),
            PlantSpec("Pinus thunbergii black pine bonsai", "Japanese Black Pine Bonsai", "Japan & Korea"),
            PlantSpec("Ulmus parvifolia chinese elm bonsai", "Chinese Elm Bonsai", "China"),
            PlantSpec("Carmona retusa fukien tea bonsai", "Fukien Tea Bonsai", "Southeast Asia"),
            PlantSpec("Adansonia digitata baobab tree africa", "African Baobab", "Sub-Saharan Africa"),
            PlantSpec("Sequoiadendron giganteum giant sequoia tree", "Giant Sequoia", "Sierra Nevada, California"),
            PlantSpec("Dracaena draco dragon blood tree", "Dragon Blood Tree", "Socotra Island"),
            PlantSpec("Ginkgo biloba maidenhair tree autumn", "Maidenhair Tree", "China"),
            PlantSpec("Jacaranda mimosifolia jacaranda tree purple", "Blue Jacaranda", "Argentina & Bolivia"),
            PlantSpec("Delonix regia flamboyant flame tree", "Flame Tree", "Madagascar"),
            PlantSpec("Eucalyptus deglupta rainbow eucalyptus tree", "Rainbow Eucalyptus", "Philippines"),
            PlantSpec("Salix babylonica weeping willow tree", "Weeping Willow", "China"),
            PlantSpec("Acer saccharum sugar maple fall", "Sugar Maple", "Eastern North America"),
            PlantSpec("Ficus benghalensis banyan tree india", "Banyan Tree", "Indian Subcontinent"),
            PlantSpec("Tabebuia rosea pink trumpet tree flower", "Pink Trumpet Tree", "Mexico to Venezuela"),
            PlantSpec("Handroanthus chrysotrichus golden trumpet tree", "Golden Trumpet Tree", "Brazil"),
            PlantSpec("Albizia julibrissin silk tree pink flower", "Silk Tree", "Asia"),
            PlantSpec("Betula pendula silver birch tree white", "Silver Birch", "Europe & Asia"),
            PlantSpec("Zelkova serrata japanese zelkova bonsai", "Japanese Zelkova Bonsai", "Japan"),
            // ── Alpine & Mountain ─────────────────────────────────────────────────────────
            PlantSpec("Leontopodium nivale edelweiss alpine flower", "Edelweiss", "Alps & Pyrenees"),
            PlantSpec("Gentiana acaulis stemless gentian alpine blue", "Stemless Gentian", "Alps"),
            PlantSpec("Papaver alpinum alpine poppy flower", "Alpine Poppy", "Alps & Arctic"),
            PlantSpec("Dryas octopetala mountain avens white flower", "Mountain Avens", "Arctic & Alpine"),
            PlantSpec("Pulsatilla vulgaris pasqueflower purple", "Pasqueflower", "Central Europe"),
            PlantSpec("Polemonium viscosum sky pilot blue flower", "Sky Pilot", "Rocky Mountains"),
            PlantSpec("Aquilegia coerulea blue columbine flower", "Blue Columbine", "Rocky Mountains"),
            PlantSpec("Erythronium grandiflorum glacier lily yellow", "Glacier Lily", "Western North America"),
            PlantSpec("Castilleja miniata indian paintbrush red", "Giant Red Paintbrush", "Western North America"),
            PlantSpec("Trollius europaeus globeflower yellow", "European Globeflower", "Northern Europe"),
            PlantSpec("Silene acaulis moss campion pink alpine", "Moss Campion", "Arctic & Alpine"),
            PlantSpec("Saxifraga oppositifolia purple saxifrage alpine", "Purple Saxifrage", "Arctic & Alpine"),
            PlantSpec("Primula farinosa bird's eye primrose pink", "Bird's Eye Primrose", "Northern Europe"),
            PlantSpec("Androsace alpina alpine rock jasmine flower", "Alpine Rock Jasmine", "Alps"),
            PlantSpec("Meconopsis betonicifolia himalayan blue poppy", "Himalayan Blue Poppy", "Himalayas"),
            // ── African Flora ─────────────────────────────────────────────────────────────
            PlantSpec("Protea cynaroides king protea flower", "King Protea", "South Africa"),
            PlantSpec("Saintpaulia ionantha african violet purple", "African Violet", "Tanzania & Kenya"),
            PlantSpec("Gerbera jamesonii barberton daisy flower", "Barberton Daisy", "South Africa"),
            PlantSpec("Zantedeschia aethiopica arum lily white", "Arum Lily", "South Africa"),
            PlantSpec("Leonotis leonurus lion's tail orange flower", "Wild Dagga", "South Africa"),
            PlantSpec("Leucospermum cordifolium pincushion protea", "Pincushion Protea", "South Africa"),
            PlantSpec("Clivia miniata bush lily orange flower", "Natal Lily", "South Africa"),
            PlantSpec("Lachenalia aloides cape cowslip yellow", "Cape Cowslip", "South Africa"),
            PlantSpec("Scadoxus multiflorus blood lily red", "Blood Lily", "Sub-Saharan Africa"),
            PlantSpec("Ornithogalum thyrsoides chincherinchee white", "Chincherinchee", "South Africa"),
            PlantSpec("Agapanthus praecox common agapanthus blue", "Common Agapanthus", "South Africa"),
            PlantSpec("Watsonia meriana bugle lily pink", "Bugle Lily", "South Africa"),
            PlantSpec("Haemanthus coccineus paintbrush lily red", "Paintbrush Lily", "South Africa"),
            PlantSpec("Leucadendron argenteum silver tree", "Silver Tree", "South Africa"),
            PlantSpec("Telopea speciosissima waratah red flower", "Waratah", "New South Wales, Australia"),
            // ── Herbs & Medicinal ─────────────────────────────────────────────────────────
            PlantSpec("Rosmarinus officinalis rosemary herb plant", "Rosemary", "Mediterranean"),
            PlantSpec("Mentha spicata spearmint herb green", "Spearmint", "Europe & Asia"),
            PlantSpec("Ocimum basilicum basil herb plant", "Sweet Basil", "Tropical Asia"),
            PlantSpec("Thymus vulgaris thyme herb garden", "Garden Thyme", "Mediterranean"),
            PlantSpec("Panax ginseng ginseng plant root", "Asian Ginseng", "Korea & China"),
            PlantSpec("Curcuma longa turmeric plant yellow", "Turmeric", "South Asia"),
            PlantSpec("Melissa officinalis lemon balm herb", "Lemon Balm", "Mediterranean"),
            PlantSpec("Hypericum perforatum st john's wort flower", "St. John's Wort", "Europe"),
            PlantSpec("Achillea millefolium yarrow white flower", "Common Yarrow", "Northern Hemisphere"),
            PlantSpec("Calendula officinalis pot marigold orange", "Pot Marigold", "Mediterranean"),
            PlantSpec("Chamaemelum nobile chamomile flower white", "Roman Chamomile", "Western Europe"),
            PlantSpec("Tanacetum parthenium feverfew white flower", "Feverfew", "Caucasus"),
            PlantSpec("Borago officinalis borage blue flower", "Borage", "Mediterranean"),
            PlantSpec("Echium vulgare viper's bugloss blue", "Viper's Bugloss", "Europe"),
            PlantSpec("Valerian officinalis valerian pink flower", "Common Valerian", "Europe & Asia"),
            // ── Mediterranean & Southern Europe ───────────────────────────────────────────
            PlantSpec("Cistus purpureus purple rockrose flower", "Purple Rock Rose", "Mediterranean"),
            PlantSpec("Lavandula stoechas french lavender purple", "French Lavender", "Mediterranean"),
            PlantSpec("Salvia officinalis common sage flower purple", "Common Sage", "Mediterranean"),
            PlantSpec("Nerium oleander oleander pink flower", "Oleander", "Mediterranean"),
            PlantSpec("Acacia dealbata mimosa yellow flower tree", "Silver Wattle", "Southeast Australia"),
            PlantSpec("Myrtus communis common myrtle white flower", "Common Myrtle", "Mediterranean"),
            PlantSpec("Olea europaea olive tree blossom", "Olive Tree", "Mediterranean"),
            PlantSpec("Spartium junceum spanish broom yellow", "Spanish Broom", "Mediterranean"),
            PlantSpec("Arbutus unedo strawberry tree flower", "Strawberry Tree", "Mediterranean"),
            PlantSpec("Cistus ladanifer gum rockrose white flower", "Gum Rock Rose", "Iberian Peninsula"),
            PlantSpec("Phlomis fruticosa jerusalem sage yellow", "Jerusalem Sage", "Mediterranean"),
            PlantSpec("Ficus carica fig tree fruit", "Common Fig", "Mediterranean & West Asia"),
            PlantSpec("Ceratonia siliqua carob tree", "Carob Tree", "Eastern Mediterranean"),
            PlantSpec("Pistacia lentiscus mastic tree green", "Mastic Tree", "Mediterranean"),
            PlantSpec("Smilax aspera common sarsaparilla vine", "Common Sarsaparilla", "Mediterranean"),
            // ── East Asian Flora ──────────────────────────────────────────────────────────
            PlantSpec("Prunus mume plum blossom chinese winter", "Chinese Plum", "China"),
            PlantSpec("Paeonia suffruticosa tree peony flower", "Tree Peony", "Northwest China"),
            PlantSpec("Osmanthus fragrans sweet olive flower", "Sweet Osmanthus", "East Asia"),
            PlantSpec("Chrysanthemum indicum wild chrysanthemum yellow", "Wild Chrysanthemum", "China & Japan"),
            PlantSpec("Loropetalum chinense chinese fringe flower", "Chinese Fringe Flower", "China"),
            PlantSpec("Enkianthus campanulatus redvein enkianthus", "Redvein Enkianthus", "Japan"),
            PlantSpec("Cercidiphyllum japonicum katsura tree", "Katsura Tree", "Japan & China"),
            PlantSpec("Pieris japonica japanese andromeda flower", "Japanese Pieris", "Japan"),
            PlantSpec("Nandina domestica sacred bamboo red berries", "Sacred Bamboo", "China to Japan"),
            PlantSpec("Weigela florida old-fashioned weigela pink", "Old-fashioned Weigela", "Northern China"),
            PlantSpec("Kolkwitzia amabilis beautybush pink flower", "Beautybush", "China"),
            PlantSpec("Rhodotypos scandens jetbead white flower", "White Jetbead", "China & Japan"),
            PlantSpec("Dicentra spectabilis bleeding heart pink", "Asian Bleeding Heart", "Siberia & China"),
            PlantSpec("Abeliophyllum distichum white forsythia", "White Forsythia", "Korea"),
            PlantSpec("Stewartia pseudocamellia stewartia white", "Japanese Stewartia", "Japan"),
            // ── Australian & Oceanian ─────────────────────────────────────────────────────
            PlantSpec("Banksia speciosa showy banksia flower", "Showy Banksia", "Western Australia"),
            PlantSpec("Grevillea robusta silky oak flower", "Silky Oak", "Eastern Australia"),
            PlantSpec("Callistemon citrinus crimson bottlebrush", "Crimson Bottlebrush", "Eastern Australia"),
            PlantSpec("Anigozanthos manglesii kangaroo paw flower", "Red & Green Kangaroo Paw", "Western Australia"),
            PlantSpec("Boronia megastigma scented boronia brown", "Brown Boronia", "Western Australia"),
            PlantSpec("Eucalyptus ficifolia red flowering gum", "Red Flowering Gum", "Western Australia"),
            PlantSpec("Xanthorrhoea australis grass tree australia", "Grass Tree", "Southern Australia"),
            PlantSpec("Leptospermum scoparium manuka flower", "Mānuka", "New Zealand & Australia"),
            PlantSpec("Metrosideros excelsa pohutukawa red flower", "Pohutukawa", "New Zealand"),
            PlantSpec("Sophora microphylla kowhai yellow flower", "Kōwhai", "New Zealand"),
            // ── North American Natives ────────────────────────────────────────────────────
            PlantSpec("Gaillardia aristata blanket flower red yellow", "Blanket Flower", "North America"),
            PlantSpec("Asclepias tuberosa butterfly weed orange", "Butterfly Weed", "Eastern North America"),
            PlantSpec("Lobelia cardinalis cardinal flower red", "Cardinal Flower", "Eastern North America"),
            PlantSpec("Penstemon digitalis beardtongue white", "Foxglove Beardtongue", "Eastern North America"),
            PlantSpec("Calochortus venustus mariposa lily", "Mariposa Lily", "California"),
            PlantSpec("Lewisia cotyledon bitterroot flower", "Siskiyou Lewisia", "Pacific Northwest"),
            PlantSpec("Clarkia amoena farewell to spring pink", "Farewell to Spring", "California"),
            PlantSpec("Oenothera biennis evening primrose yellow", "Common Evening Primrose", "Eastern North America"),
            PlantSpec("Trillium erectum purple trillium flower", "Purple Trillium", "Eastern North America"),
            PlantSpec("Sanguinaria canadensis bloodroot white flower", "Bloodroot", "Eastern North America"),
            PlantSpec("Dicentra cucullaria dutchman's breeches white", "Dutchman's Breeches", "North America"),
            PlantSpec("Claytonia virginica spring beauty flower", "Virginia Spring Beauty", "Eastern North America"),
            PlantSpec("Podophyllum peltatum mayapple flower", "Mayapple", "Eastern North America"),
            PlantSpec("Mimulus guttatus monkey flower yellow", "Common Monkey Flower", "Western North America"),
            PlantSpec("Romneya coulteri matilija poppy white", "Matilija Poppy", "California & Baja"),
            // ── South American Gems ───────────────────────────────────────────────────────
            PlantSpec("Cantua buxifolia sacred flower inca peru", "Sacred Flower of the Incas", "Andes"),
            PlantSpec("Puya raimondii queen of the andes flower", "Queen of the Andes", "Bolivian Andes"),
            PlantSpec("Neomarica gracilis walking iris blue", "Walking Iris", "Brazil"),
            PlantSpec("Cuphea ignea cigar plant orange", "Cigar Flower", "Mexico & Jamaica"),
            PlantSpec("Tibouchina urvilleana glory bush purple", "Princess Flower", "Brazil"),
            PlantSpec("Mandevilla sanderi brazilian jasmine pink", "Brazilian Jasmine", "Brazil"),
            PlantSpec("Aristolochia gigantea dutchman pipe flower", "Giant Dutchman's Pipe", "Brazil"),
            PlantSpec("Tropaeolum speciosum flame nasturtium red", "Flame Nasturtium", "Chile"),
            PlantSpec("Alstroemeria ligtu chilean lily flower", "Chilean Lily", "Chile & Argentina"),
            PlantSpec("Combretum rotundifolium monkey brush flower", "Monkey Brush Vine", "Amazon Basin"),
            // ── European Wildflowers ──────────────────────────────────────────────────────
            PlantSpec("Papaver rhoeas common poppy red field", "Common Poppy", "Mediterranean & Europe"),
            PlantSpec("Bellis perennis common daisy white", "Common Daisy", "Europe"),
            PlantSpec("Centaurea cyanus cornflower blue", "Cornflower", "Europe"),
            PlantSpec("Leucanthemum vulgare oxeye daisy field", "Oxeye Daisy", "Europe"),
            PlantSpec("Ranunculus acris meadow buttercup yellow", "Meadow Buttercup", "Europe"),
            PlantSpec("Campanula rotundifolia harebell blue flower", "Harebell", "Northern Hemisphere"),
            PlantSpec("Filipendula ulmaria meadowsweet white flower", "Meadowsweet", "Europe & Asia"),
            PlantSpec("Astrantia major masterwort flower", "Great Masterwort", "Central Europe"),
            PlantSpec("Geranium pratense meadow cranesbill blue", "Meadow Cranesbill", "Europe & Asia"),
            PlantSpec("Knautia arvensis field scabious purple", "Field Scabious", "Europe"),
            PlantSpec("Centaurea scabiosa greater knapweed purple", "Greater Knapweed", "Europe"),
            PlantSpec("Cichorium intybus chicory blue flower", "Common Chicory", "Europe & Asia"),
            PlantSpec("Eupatorium cannabinum hemp agrimony pink", "Hemp Agrimony", "Europe & Asia"),
            PlantSpec("Angelica sylvestris wild angelica white", "Wild Angelica", "Europe"),
            PlantSpec("Linaria vulgaris common toadflax yellow", "Common Toadflax", "Europe & Asia"),
            PlantSpec("Silene dioica red campion flower", "Red Campion", "Europe"),
            PlantSpec("Lychnis flos-cuculi ragged robin pink", "Ragged Robin", "Europe & Asia"),
            PlantSpec("Menyanthes trifoliata bogbean white flower", "Bogbean", "Northern Hemisphere"),
            PlantSpec("Viola tricolor heartsease wild pansy", "Wild Pansy", "Europe & Asia"),
            PlantSpec("Oxalis acetosella wood sorrel white flower", "Wood Sorrel", "Northern Hemisphere"),
            // ── Mosses & Extraordinary Plants ────────────────────────────────────────────
            PlantSpec("Sphagnum moss bog peat green", "Peat Moss", "Northern Hemisphere Bogs"),
            PlantSpec("Polytrichum commune haircap moss forest", "Common Haircap Moss", "Worldwide"),
            PlantSpec("Cladonia rangiferina reindeer lichen arctic", "Reindeer Lichen", "Arctic & Subarctic"),
            PlantSpec("Lobaria pulmonaria lungwort lichen tree", "Lungwort Lichen", "Europe & North America"),
            PlantSpec("Marchantia polymorpha liverwort green", "Common Liverwort", "Worldwide"),
            PlantSpec("Welwitschia mirabilis ancient desert plant", "Welwitschia", "Namib Desert"),
            PlantSpec("Amorphophallus titanum titan arum corpse flower", "Corpse Flower", "Sumatra"),
            PlantSpec("Rafflesia arnoldii world's largest flower", "Rafflesia", "Borneo & Sumatra"),
            PlantSpec("Punica granatum pomegranate flower orange", "Pomegranate", "Iran & Himalayas"),
            PlantSpec("Victoria cruziana santa cruz water lily", "Santa Cruz Water Lily", "South America"),
            // ── Cherry Blossom Varieties ──────────────────────────────────────────────────
            PlantSpec("Prunus x yedoensis yoshino cherry blossom", "Yoshino Cherry", "Japan"),
            PlantSpec("Prunus avium wild cherry blossom white", "Wild Cherry", "Europe & Asia"),
            PlantSpec("Prunus 'Kanzan' double pink cherry blossom", "Kanzan Cherry", "Japan"),
            PlantSpec("Prunus subhirtella spring cherry pale pink", "Spring Cherry", "Japan"),
            PlantSpec("Prunus cerasifera cherry plum blossom", "Cherry Plum", "Western Asia"),
            PlantSpec("Prunus padus bird cherry blossom white", "Bird Cherry", "Europe & Asia"),
            PlantSpec("Prunus campanulata bell-flowered cherry red", "Bell-flowered Cherry", "Taiwan & Japan"),
            PlantSpec("Prunus sargentii sargent cherry pink", "Sargent's Cherry", "Japan & Korea"),
            // ── Lavender Varieties ────────────────────────────────────────────────────────
            PlantSpec("Lavandula angustifolia true lavender purple", "True Lavender", "Western Mediterranean"),
            PlantSpec("Lavandula dentata fringed lavender flower", "Fringed Lavender", "Mediterranean"),
            PlantSpec("Lavandula x intermedia lavandin field", "Lavandin", "Mediterranean"),
            PlantSpec("Lavandula multifida fernleaf lavender", "Fernleaf Lavender", "Mediterranean"),
            PlantSpec("Lavandula lanata woolly lavender silver", "Woolly Lavender", "Spain"),
            // ── Magnolia Varieties ────────────────────────────────────────────────────────
            PlantSpec("Magnolia grandiflora southern magnolia white", "Southern Magnolia", "Southeastern USA"),
            PlantSpec("Magnolia liliiflora purple magnolia flower", "Mulan Magnolia", "China"),
            PlantSpec("Magnolia x soulangeana saucer magnolia pink", "Saucer Magnolia", "Garden origin"),
            PlantSpec("Magnolia campbellii pink himalayan magnolia", "Campbell's Magnolia", "Himalayas"),
            PlantSpec("Magnolia denudata yulan magnolia white", "Yulan Magnolia", "China"),
            PlantSpec("Magnolia sieboldii oyama magnolia white", "Oyama Magnolia", "Japan & Korea"),
            // ── Lotus & Water Lily Varieties ──────────────────────────────────────────────
            PlantSpec("Nelumbo 'Momo Botan' double pink lotus", "Momo Botan Lotus", "East Asia (cultivar)"),
            PlantSpec("Nelumbo nucifera alba white sacred lotus", "White Sacred Lotus", "India & East Asia"),
            PlantSpec("Nelumbo lutea american yellow lotus", "American Yellow Lotus", "North America"),
            PlantSpec("Nymphaea 'Black Princess' dark water lily", "Black Princess Lily", "Garden cultivar"),
            PlantSpec("Nymphaea 'Attraction' pink water lily", "Attraction Water Lily", "Garden cultivar"),
            // ── Rare & Unusual ────────────────────────────────────────────────────────────
            PlantSpec("Psychotria elata hot lips plant flower", "Hot Lips Plant", "Central & South America"),
            PlantSpec("Impatiens noli-tangere yellow balsam flower", "Yellow Balsam", "Europe & Asia"),
            PlantSpec("Dictamnus albus burning bush plant white", "Gas Plant", "Europe & Asia"),
            PlantSpec("Lavatera trimestris rose mallow pink", "Annual Mallow", "Mediterranean"),
            PlantSpec("Nolana paradoxa chilean bellflower blue", "Chilean Bellflower", "Chile & Peru"),
            PlantSpec("Doronicum orientale leopard's bane yellow", "Caucasian Leopard's Bane", "Caucasus"),
            PlantSpec("Rodgersia pinnata featherleaf rodgersia pink", "Featherleaf Rodgersia", "China"),
            PlantSpec("Dierama pulcherrimum wand flower pink", "Wandflower", "South Africa"),
            PlantSpec("Eucomis comosa pineapple lily flower", "Pineapple Lily", "South Africa"),
            PlantSpec("Primula auricula auricula primrose flower", "Auricula Primrose", "Alps"),
            PlantSpec("Actaea racemosa black cohosh flower", "Black Cohosh", "Eastern North America"),
            PlantSpec("Filipendula rubra queen of the prairie", "Queen of the Prairie", "Eastern North America"),
            PlantSpec("Thalictrum aquilegiifolium meadow rue purple", "Columbine Meadow-rue", "Europe & Asia"),
            PlantSpec("Sanguisorba officinalis great burnet red", "Great Burnet", "Europe & Asia"),
            PlantSpec("Epimedium youngianum barrenwort flower", "Young's Barrenwort", "Japan & China"),
            PlantSpec("Helleborus niger christmas rose white", "Christmas Rose", "Alps & Balkans"),
            PlantSpec("Trollius chinensis chinese globeflower", "Chinese Globeflower", "Northeast China"),
            PlantSpec("Meconopsis cambrica welsh poppy yellow", "Welsh Poppy", "Western Europe"),
            PlantSpec("Acanthus spinosus spiny bear's breeches", "Spiny Bear's Breeches", "Mediterranean"),
            PlantSpec("Verbascum olympicum olympic mullein yellow", "Olympic Mullein", "Greece & Turkey")
        )

        /** Category map for browsing — keys match internal category identifiers. */
        private val CATEGORY_PLANTS: Map<String, List<PlantSpec>> = mapOf(
            "wildflower" to listOf(
                PlantSpec("Papaver rhoeas common poppy red", "Common Poppy", "Mediterranean & Europe"),
                PlantSpec("Bellis perennis daisy meadow", "Common Daisy", "Europe"),
                PlantSpec("Centaurea cyanus cornflower blue", "Cornflower", "Europe"),
                PlantSpec("Leucanthemum vulgare oxeye daisy", "Oxeye Daisy", "Europe"),
                PlantSpec("Lotus corniculatus trefoil yellow", "Bird's-foot Trefoil", "Europe"),
                PlantSpec("Ranunculus acris buttercup yellow", "Meadow Buttercup", "Europe"),
                PlantSpec("Campanula rotundifolia harebell blue", "Harebell", "Northern Hemisphere"),
                PlantSpec("Prunella vulgaris selfheal purple", "Self-heal", "Northern Hemisphere"),
                PlantSpec("Filipendula ulmaria meadowsweet", "Meadowsweet", "Europe & Asia"),
                PlantSpec("Astrantia major masterwort", "Great Masterwort", "Central Europe"),
                PlantSpec("Geranium pratense meadow cranesbill", "Meadow Cranesbill", "Europe & Asia"),
                PlantSpec("Silene dioica red campion", "Red Campion", "Europe"),
                PlantSpec("Viola tricolor heartsease", "Wild Pansy", "Europe & Asia"),
                PlantSpec("Lychnis flos-cuculi ragged robin", "Ragged Robin", "Europe & Asia"),
                PlantSpec("Cichorium intybus chicory blue", "Common Chicory", "Europe & Asia"),
                PlantSpec("Knautia arvensis field scabious", "Field Scabious", "Europe"),
                PlantSpec("Centaurea scabiosa knapweed", "Greater Knapweed", "Europe"),
                PlantSpec("Linaria vulgaris toadflax yellow", "Common Toadflax", "Europe & Asia"),
                PlantSpec("Menyanthes trifoliata bogbean", "Bogbean", "Northern Hemisphere"),
                PlantSpec("Oxalis acetosella wood sorrel", "Wood Sorrel", "Northern Hemisphere")
            ),
            "tropical" to listOf(
                PlantSpec("Strelitzia reginae bird of paradise", "Bird of Paradise", "South Africa"),
                PlantSpec("Heliconia rostrata lobster claw", "Lobster Claw Heliconia", "South America"),
                PlantSpec("Plumeria frangipani tropical flower", "Frangipani", "Mexico & Caribbean"),
                PlantSpec("Anthurium andreanum flamingo lily", "Flamingo Lily", "Colombia"),
                PlantSpec("Hibiscus rosa-sinensis hibiscus", "Chinese Hibiscus", "East Asia"),
                PlantSpec("Alpinia purpurata red ginger", "Red Torch Ginger", "Malaysia"),
                PlantSpec("Calathea orbifolia tropical leaf", "Calathea", "Bolivia"),
                PlantSpec("Monstera deliciosa swiss cheese plant", "Swiss Cheese Plant", "Southern Mexico"),
                PlantSpec("Medinilla magnifica rose grape", "Rose Grape", "Philippines"),
                PlantSpec("Gloriosa superba flame lily", "Flame Lily", "Tropical Africa & Asia"),
                PlantSpec("Costus speciosus spiral ginger", "Spiral Ginger", "Southeast Asia"),
                PlantSpec("Strongylodon macrobotrys jade vine", "Jade Vine", "Philippines"),
                PlantSpec("Tacca chantrieri black bat plant", "Black Bat Plant", "Southeast Asia"),
                PlantSpec("Thunbergia grandiflora sky vine", "Sky Vine", "India"),
                PlantSpec("Alpinia zerumbet shell ginger", "Shell Ginger", "East Asia"),
                PlantSpec("Hedychium gardnerianum kahili ginger", "Kahili Ginger", "Himalayas"),
                PlantSpec("Ravenala madagascariensis travelers palm", "Traveller's Palm", "Madagascar"),
                PlantSpec("Canna indica indian shot", "Indian Shot", "South America"),
                PlantSpec("Tibouchina urvilleana glory bush", "Princess Flower", "Brazil"),
                PlantSpec("Combretum rotundifolium monkey brush", "Monkey Brush Vine", "Amazon Basin")
            ),
            "fern" to listOf(
                PlantSpec("Dryopteris filix-mas male fern forest", "Male Fern", "Northern Hemisphere"),
                PlantSpec("Osmunda regalis royal fern", "Royal Fern", "Europe, Asia & Americas"),
                PlantSpec("Asplenium nidus bird's nest fern", "Bird's Nest Fern", "Tropical Asia"),
                PlantSpec("Polypodium vulgare common polypody", "Common Polypody", "Europe"),
                PlantSpec("Athyrium filix-femina lady fern", "Lady Fern", "Northern Hemisphere"),
                PlantSpec("Adiantum capillus-veneris maidenhair fern", "Maidenhair Fern", "Worldwide Tropics"),
                PlantSpec("Cyathea medullaris tree fern", "Black Tree Fern", "New Zealand"),
                PlantSpec("Matteuccia struthiopteris ostrich fern", "Ostrich Fern", "Northern Hemisphere"),
                PlantSpec("Platycerium bifurcatum staghorn fern", "Staghorn Fern", "Eastern Australia"),
                PlantSpec("Selaginella lepidophylla resurrection plant", "Resurrection Plant", "Chihuahuan Desert"),
                PlantSpec("Nephrolepis exaltata boston fern", "Boston Fern", "Tropical Regions"),
                PlantSpec("Phlebodium aureum blue star fern", "Blue Star Fern", "Tropical Americas"),
                PlantSpec("Dicksonia antarctica soft tree fern", "Soft Tree Fern", "Eastern Australia"),
                PlantSpec("Blechnum spicant hard fern", "Hard Fern", "Northern Hemisphere"),
                PlantSpec("Polystichum setiferum shield fern", "Soft Shield Fern", "Europe")
            ),
            "succulent" to listOf(
                PlantSpec("Echeveria elegans mexican snowball", "Mexican Snowball", "Mexico"),
                PlantSpec("Aloe vera plant green", "Aloe Vera", "Arabian Peninsula"),
                PlantSpec("Sedum acre stonecrop yellow", "Biting Stonecrop", "Europe & Asia"),
                PlantSpec("Sempervivum tectorum houseleek", "Common Houseleek", "Mountains of Europe"),
                PlantSpec("Agave americana century plant", "Century Plant", "Mexico"),
                PlantSpec("Haworthia attenuata zebra plant", "Zebra Haworthia", "South Africa"),
                PlantSpec("Crassula ovata jade plant", "Jade Plant", "South Africa"),
                PlantSpec("Dudleya brittonii chalk liveforever", "Chalk Liveforever", "Baja California"),
                PlantSpec("Lithops optica living stones", "Living Stone", "Namibia & South Africa"),
                PlantSpec("Kalanchoe blossfeldiana flaming katy", "Flaming Katy", "Madagascar"),
                PlantSpec("Aeonium arboreum tree aeonium", "Tree Aeonium", "Canary Islands"),
                PlantSpec("Aloe ferox cape aloe", "Cape Aloe", "South Africa"),
                PlantSpec("Stapelia gigantea starfish cactus", "Starfish Cactus", "South Africa"),
                PlantSpec("Schlumbergera truncata christmas cactus", "Christmas Cactus", "Brazil")
            ),
            "orchid" to listOf(
                PlantSpec("Phalaenopsis amabilis moth orchid white", "Moth Orchid", "Southeast Asia"),
                PlantSpec("Cattleya labiata corsage orchid purple", "Corsage Orchid", "Brazil"),
                PlantSpec("Dendrobium nobile noble dendrobium", "Noble Dendrobium", "Himalayas to China"),
                PlantSpec("Vanda coerulea blue vanda orchid", "Blue Vanda Orchid", "Northeast India"),
                PlantSpec("Ophrys apifera bee orchid wild", "Bee Orchid", "Europe & Mediterranean"),
                PlantSpec("Dactylorhiza fuchsii spotted orchid", "Common Spotted Orchid", "Europe"),
                PlantSpec("Paphiopedilum slipper orchid", "Lady's Slipper Orchid", "Southeast Asia"),
                PlantSpec("Coelogyne cristata necklace orchid", "Necklace Orchid", "Himalayas"),
                PlantSpec("Dracula simia monkey face orchid", "Monkey Face Orchid", "Ecuador & Peru"),
                PlantSpec("Vanilla planifolia vanilla orchid", "Vanilla Orchid", "Mexico"),
                PlantSpec("Zygopetalum orchid blue purple", "Zygopetalum Orchid", "South America"),
                PlantSpec("Maxillaria tenuifolia coconut orchid", "Coconut Orchid", "Mexico to Colombia"),
                PlantSpec("Prosthechea cochleata clamshell orchid", "Clamshell Orchid", "Mexico & Caribbean")
            ),
            "bonsai" to listOf(
                PlantSpec("Juniperus procumbens nana bonsai", "Juniper Bonsai", "Japan"),
                PlantSpec("Ficus retusa ginseng bonsai", "Ginseng Ficus Bonsai", "Southeast Asia"),
                PlantSpec("Acer palmatum maple bonsai", "Japanese Maple Bonsai", "Japan & Korea"),
                PlantSpec("Pinus thunbergii black pine bonsai", "Japanese Black Pine Bonsai", "Japan & Korea"),
                PlantSpec("Prunus mume ume apricot bonsai", "Japanese Apricot Bonsai", "China"),
                PlantSpec("Zelkova serrata bonsai", "Japanese Zelkova Bonsai", "Japan"),
                PlantSpec("Carmona retusa fukien tea bonsai", "Fukien Tea Bonsai", "Southeast Asia"),
                PlantSpec("Ulmus parvifolia chinese elm bonsai", "Chinese Elm Bonsai", "China"),
                PlantSpec("Serissa japonica snowrose bonsai", "Snow Rose Bonsai", "Japan & China"),
                PlantSpec("Cotoneaster horizontalis bonsai", "Cotoneaster Bonsai", "China")
            ),
            "moss" to listOf(
                PlantSpec("Bryum argenteum silver moss closeup", "Silver-green Bryum", "Worldwide"),
                PlantSpec("Sphagnum moss bog peat", "Peat Moss", "Northern Hemisphere Bogs"),
                PlantSpec("Hypnum cupressiforme cypress moss", "Cypress-leaved Moss", "Worldwide"),
                PlantSpec("Polytrichum commune haircap moss", "Common Haircap Moss", "Worldwide"),
                PlantSpec("Plagiomnium undulatum wavy feather moss", "Wavy Feather Moss", "Europe"),
                PlantSpec("Dicranum scoparium broom fork-moss", "Broom Fork Moss", "Northern Hemisphere"),
                PlantSpec("Thuidium tamariscinum tamarisk moss", "Tamarisk Moss", "Europe"),
                PlantSpec("Leucobryum glaucum white cushion moss", "White Cushion Moss", "Europe & North America"),
                PlantSpec("Cladonia rangiferina reindeer lichen", "Reindeer Lichen", "Arctic & Subarctic"),
                PlantSpec("Marchantia polymorpha liverwort", "Common Liverwort", "Worldwide")
            ),
            "water lily" to listOf(
                PlantSpec("Nymphaea alba white water lily pond", "White Water Lily", "Europe & North Africa"),
                PlantSpec("Nuphar lutea yellow water lily", "Yellow Water Lily", "Europe & Western Asia"),
                PlantSpec("Victoria amazonica giant water lily", "Giant Amazon Water Lily", "Amazon Basin"),
                PlantSpec("Nelumbo nucifera sacred lotus pink", "Sacred Lotus", "India & East Asia"),
                PlantSpec("Nymphaea caerulea blue lotus egypt", "Blue Lotus", "Egypt & East Africa"),
                PlantSpec("Euryale ferox foxnut water plant", "Foxnut", "East Asia"),
                PlantSpec("Nelumbo lutea american yellow lotus", "American Lotus", "North America"),
                PlantSpec("Victoria cruziana water lily", "Santa Cruz Water Lily", "South America")
            ),
            "cactus" to listOf(
                PlantSpec("Cereus repandus columnar cactus", "Peruvian Apple Cactus", "South America"),
                PlantSpec("Opuntia ficus-indica prickly pear", "Prickly Pear Cactus", "Mexico"),
                PlantSpec("Echinocactus grusonii golden barrel", "Golden Barrel Cactus", "Central Mexico"),
                PlantSpec("Ferocactus cylindraceus barrel cactus", "California Barrel Cactus", "Sonoran Desert"),
                PlantSpec("Carnegiea gigantea saguaro cactus", "Saguaro Cactus", "Sonoran Desert"),
                PlantSpec("Mammillaria hahniana old lady cactus", "Old Lady Cactus", "Mexico"),
                PlantSpec("Astrophytum myriostigma bishop hat", "Bishop's Hat Cactus", "Mexico"),
                PlantSpec("Echinocereus triglochidiatus claret cup", "Claret Cup Cactus", "Southwestern USA"),
                PlantSpec("Gymnocalycium mihanovichii moon cactus", "Moon Cactus", "South America"),
                PlantSpec("Trichocereus pachanoi san pedro cactus", "San Pedro Cactus", "Andes")
            ),
            "cherry blossom" to listOf(
                PlantSpec("Prunus serrulata Yoshino cherry blossom", "Yoshino Cherry", "Japan"),
                PlantSpec("Prunus x yedoensis tokyo cherry blossom", "Tokyo Cherry", "Japan"),
                PlantSpec("Prunus avium wild cherry blossom", "Wild Cherry", "Europe & Asia"),
                PlantSpec("Prunus 'Kanzan' double pink cherry", "Kanzan Cherry", "Japan"),
                PlantSpec("Prunus subhirtella spring cherry pale", "Spring Cherry", "Japan"),
                PlantSpec("Prunus cerasifera myrobalan plum", "Cherry Plum", "Western Asia"),
                PlantSpec("Prunus padus bird cherry white", "Bird Cherry", "Europe & Asia"),
                PlantSpec("Prunus campanulata bell-flowered cherry", "Bell-flowered Cherry", "Taiwan & Japan")
            ),
            "lavender" to listOf(
                PlantSpec("Lavandula angustifolia true lavender", "True Lavender", "Western Mediterranean"),
                PlantSpec("Lavandula stoechas French lavender", "French Lavender", "Mediterranean"),
                PlantSpec("Lavandula dentata fringed lavender", "Fringed Lavender", "Mediterranean"),
                PlantSpec("Lavandula x intermedia lavandin", "Lavandin", "Mediterranean"),
                PlantSpec("Lavandula multifida fernleaf lavender", "Fernleaf Lavender", "Mediterranean"),
                PlantSpec("Lavandula lanata woolly lavender", "Woolly Lavender", "Spain"),
                PlantSpec("Lavandula latifolia spike lavender", "Spike Lavender", "Mediterranean"),
                PlantSpec("Lavandula canariensis canary lavender", "Canary Lavender", "Canary Islands")
            ),
            "sunflower" to listOf(
                PlantSpec("Helianthus annuus common sunflower", "Common Sunflower", "North America"),
                PlantSpec("Helianthus debilis beach sunflower", "Beach Sunflower", "North America"),
                PlantSpec("Helianthus tuberosus jerusalem artichoke", "Jerusalem Artichoke", "North America"),
                PlantSpec("Helianthus 'Teddy Bear' double sunflower", "Teddy Bear Sunflower", "Garden cultivar"),
                PlantSpec("Heliopsis helianthoides false sunflower", "Smooth Oxeye", "North America"),
                PlantSpec("Helianthus 'Moulin Rouge' red sunflower", "Moulin Rouge Sunflower", "Garden cultivar"),
                PlantSpec("Rudbeckia hirta black eyed susan", "Black-eyed Susan", "North America"),
                PlantSpec("Helenium autumnale sneezeweed orange", "Common Sneezeweed", "North America")
            ),
            "magnolia" to listOf(
                PlantSpec("Magnolia grandiflora southern magnolia", "Southern Magnolia", "Southeastern USA"),
                PlantSpec("Magnolia stellata star magnolia", "Star Magnolia", "Japan"),
                PlantSpec("Magnolia liliiflora purple magnolia", "Mulan Magnolia", "China"),
                PlantSpec("Magnolia x soulangeana saucer magnolia", "Saucer Magnolia", "Garden origin"),
                PlantSpec("Magnolia sieboldii oyama magnolia", "Oyama Magnolia", "Japan & Korea"),
                PlantSpec("Magnolia campbellii pink himalayan", "Campbell's Magnolia", "Himalayas"),
                PlantSpec("Magnolia obovata Japanese magnolia", "Japanese Big-leaf Magnolia", "Japan"),
                PlantSpec("Magnolia denudata yulan magnolia", "Yulan Magnolia", "China")
            ),
            "lotus" to listOf(
                PlantSpec("Nelumbo nucifera pink sacred lotus", "Sacred Lotus", "India & East Asia"),
                PlantSpec("Nelumbo lutea American yellow lotus", "American Lotus", "North America"),
                PlantSpec("Nelumbo 'Momo Botan' double lotus", "Momo Botan Lotus", "East Asia (cultivar)"),
                PlantSpec("Nelumbo nucifera alba white lotus", "White Sacred Lotus", "India & East Asia"),
                PlantSpec("Nymphaea caerulea blue lotus egypt", "Blue Lotus", "Egypt & East Africa"),
                PlantSpec("Victoria amazonica giant water lily", "Giant Amazon Water Lily", "Amazon Basin"),
                PlantSpec("Nelumbo 'Carolina Queen' lotus", "Carolina Queen Lotus", "Garden cultivar"),
                PlantSpec("Nelumbo 'Baby Doll' miniature lotus", "Baby Doll Lotus", "Garden cultivar")
            )
        )

        private val ALL_PLANTS: List<PlantSpec> = CATEGORY_PLANTS.values.flatten()
    }

    fun getTodayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    suspend fun getTodayPlant(): DailyPlant? = dao.getByDate(getTodayKey())

    suspend fun getPlantByKey(dateKey: String): DailyPlant? = dao.getByDate(dateKey)

    suspend fun getHistory(): List<DailyPlant> = dao.getHistory()

    suspend fun getFavorites(): List<DailyPlant> = dao.getFavorites()

    suspend fun getAllForSeasonal(): List<DailyPlant> = dao.getAllForSeasonal()

    suspend fun toggleFavorite(plant: DailyPlant) {
        dao.setFavorite(plant.dateKey, !plant.isFavorite)
    }

    suspend fun updateNotes(dateKey: String, notes: String?) {
        dao.updateNotes(dateKey, notes)
    }

    suspend fun fetchAndSaveTodayPlant(): DailyPlant {
        val dateKey = getTodayKey()
        val existing = dao.getByDate(dateKey)
        if (existing != null) return existing

        val spec = pickPlantForToday()
        return fetchAndSave(dateKey, spec.query, spec.name, spec.region)
    }

    /**
     * Fetches a plant for a given time-slot seed (used by premium auto-refresh wallpaper).
     * Each slot picks a different plant from DAILY_PLANTS, cycling through all 406.
     * Caches per slot so repeated calls within the same interval window reuse the result.
     */
    suspend fun fetchWallpaperPlantForSlot(slotSeed: Long): DailyPlant {
        val idx = ((slotSeed % DAILY_PLANTS.size) + DAILY_PLANTS.size).toInt() % DAILY_PLANTS.size
        val spec = DAILY_PLANTS[idx]
        val dateKey = "wallpaper-slot-$slotSeed"
        val existing = dao.getByDate(dateKey)
        if (existing != null) return existing
        return fetchAndSave(dateKey, spec.query, spec.name, spec.region)
    }

    suspend fun fetchForCategory(categoryQuery: String): DailyPlant {
        val dateKey = "${getTodayKey()}-$categoryQuery-${System.currentTimeMillis()}"
        val plants = CATEGORY_PLANTS[categoryQuery] ?: ALL_PLANTS
        val pick = plants[(System.currentTimeMillis() / 1000).toInt() % plants.size]
        return fetchAndSave(dateKey, pick.query, pick.name, pick.region, forceNew = true)
    }

    /**
     * Picks today's plant from the DAILY_PLANTS list using the day-of-year index.
     * With 406 plants this guarantees over 1 full year of unique daily discoveries.
     */
    private fun pickPlantForToday(): PlantSpec {
        val dayOfYear = SimpleDateFormat("D", Locale.US).format(Date()).toInt()
        return DAILY_PLANTS[(dayOfYear - 1) % DAILY_PLANTS.size]
    }

    /** Try Unsplash, returning null on any failure so callers can cascade fallbacks. */
    private suspend fun tryUnsplashPhoto(query: String): UnsplashPhoto? = try {
        unsplashApi.getRandomPhoto(query = query)
    } catch (e: Exception) {
        Log.w(TAG, "Unsplash miss for '$query': ${e.message}")
        null
    }

    private suspend fun fetchAndSave(
        dateKey: String,
        query: String,
        displayName: String,
        nativeRegion: String,
        forceNew: Boolean = false
    ): DailyPlant {
        // Cascade: specific query → common name → generic botanical
        val photo = tryUnsplashPhoto(query)
            ?: tryUnsplashPhoto(displayName)
            ?: tryUnsplashPhoto("botanical flower garden")
            ?: run {
                Log.e(TAG, "All Unsplash queries failed for '$displayName'")
                throw Exception("No photo found for $displayName")
            }

        val location = buildLocationString(photo)
        val (insight, scientificName) = fetchBotanicalInsight(displayName, nativeRegion)

        val plant = DailyPlant(
            dateKey = dateKey,
            photoId = photo.id,
            imageUrlFull = photo.urls.full,
            imageUrlRegular = photo.urls.regular,
            plantName = displayName,
            scientificName = scientificName,
            locationName = location,
            photographerName = photo.user.name,
            photographerUsername = photo.user.username,
            photographerProfileUrl = photo.user.links.html,
            downloadLocationUrl = photo.links.downloadLocation,
            botanicalInsight = insight,
            nativeRegion = nativeRegion
        )

        dao.insert(plant)
        if (!forceNew) pruneOldEntries()
        return plant
    }

    private suspend fun pruneOldEntries() {
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        dao.pruneOlderThan(sevenDaysAgo)
    }

    private fun buildLocationString(photo: UnsplashPhoto): String? {
        val loc = photo.location ?: return null
        return listOfNotNull(loc.name, loc.city, loc.country)
            .filter { it.isNotBlank() }
            .firstOrNull()
    }

    /**
     * Fetches botanical insight and scientific name from the FloraFlow backend,
     * which proxies the call to OpenAI server-side. No API key on the device.
     */
    private suspend fun fetchBotanicalInsight(
        plantName: String,
        nativeRegion: String
    ): Pair<String, String?> {
        return try {
            val response = floraFlowApi.getBotanicalInsight(
                InsightRequest(plantName, nativeRegion)
            )
            Pair(
                response.insight ?: "A remarkable plant with a fascinating botanical story.",
                response.scientificName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Insight fetch failed for '$plantName'", e)
            Pair("This plant has captivated botanists and naturalists for centuries with its unique characteristics and ecological role.", null)
        }
    }

    suspend fun fetchBotanicalQuiz(plant: DailyPlant): com.floraflow.app.api.QuizResponse {
        return floraFlowApi.getBotanicalQuiz(
            QuizRequest(plant.plantName, plant.scientificName)
        )
    }

    /** Returns QuizData (domain model) or null on failure — used by QuizViewModel. */
    suspend fun generateQuiz(plant: DailyPlant): QuizData? = try {
        val resp = fetchBotanicalQuiz(plant)
        if (resp.question != null && resp.options != null && resp.correct != null && resp.explanation != null) {
            QuizData(
                question    = resp.question,
                options     = resp.options,
                correct     = resp.correct,
                explanation = resp.explanation,
                dateKey     = getTodayKey()
            )
        } else null
    } catch (_: Exception) { null }

    /** Triggers an Unsplash download event (required for attribution). */
    suspend fun triggerDownload(url: String) {
        try { unsplashApi.triggerDownload(url) } catch (_: Exception) {}
    }
}
