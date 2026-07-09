package com.genapp.core.generator;

import com.genapp.core.model.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Code Generator untuk Android Jetpack Compose (Kotlin)
 */
@Component
public class ComposeCodeGenerator implements CodeGenerator {

    @Override
    public String generateAppCode(Aplikasi aplikasi) {
        StringBuilder code = new StringBuilder();
        
        // Generate MainActivity
        code.append(generateMainActivity(aplikasi));
        code.append("\n\n");
        
        // Generate screens
        for (int i = 0; i < aplikasi.screens().size(); i++) {
            code.append(generateScreenCode(aplikasi, i));
            code.append("\n\n");
        }
        
        // Generate ViewModel
        for (Layar screen : aplikasi.screens()) {
            code.append(generateViewModel(screen.nama(), aplikasi));
            code.append("\n\n");
        }
        
        return code.toString();
    }

    @Override
    public String generateScreenCode(Aplikasi aplikasi, int screenIndex) {
        Layar screen = aplikasi.screens().get(screenIndex);
        StringBuilder code = new StringBuilder();
        
        code.append("@Composable\n");
        code.append("fun ").append(screen.nama()).append("Screen(\n");
        code.append("    viewModel: ").append(screen.nama()).append("ViewModel,\n");
        code.append("    navigator: Navigator\n");
        code.append(") {\n");
        code.append("    val uiState by viewModel.uiState.collectAsState()\n");
        code.append("    \n");
        
        // Add monetization code if enabled
        if (aplikasi.monetization() != null && aplikasi.monetization().enabled()) {
            code.append("    // AdMob Banner Ad\n");
            code.append("    var showBannerAd by remember { mutableStateOf(true) }\n");
            code.append("    if (showBannerAd) {\n");
            code.append("        AdBanner(\n");
            code.append("            adUnitId = \"").append(aplikasi.monetization().bannerAdUnitId() != null ? aplikasi.monetization().bannerAdUnitId() : "ca-app-pub-XXXXXXXXXXXXXXXX/BBBBBBBBBB").append("\",\n");
            code.append("            onAdLoaded = { showBannerAd = true },\n");
            code.append("            onAdFailedToLoad = { showBannerAd = false }\n");
            code.append("        )\n");
            code.append("    }\n");
            code.append("    \n");
        }
        
        // Generate screen content based on type
        code.append(generateScreenContent(screen));
        
        code.append("}\n");
        
        return code.toString();
    }

    private String generateScreenContent(Layar screen) {
        StringBuilder code = new StringBuilder();
        
        switch (screen.tipe()) {
            case SPLASH:
                code.append(generateSplashScreen());
                break;
            case LOGIN:
                code.append(generateLoginScreen(screen));
                break;
            case HOME:
                code.append(generateHomeScreen(screen));
                break;
            case PRODUCT_DETAIL:
                code.append(generateProductDetailScreen(screen));
                break;
            case CART:
                code.append(generateCartScreen(screen));
                break;
            default:
                code.append(generateGenericScreen(screen));
        }
        
        return code.toString();
    }

    private String generateSplashScreen() {
        return """
            LaunchedEffect(Unit) {
                delay(2000)
                navigator.navigate(Screen.Home)
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "GenApp",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Blue
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            }
            """;
    }

    private String generateLoginScreen(Layar screen) {
        StringBuilder code = new StringBuilder();
        code.append("""
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            """);
        
        // Add components from screen definition
        for (Komponen komponen : screen.komponen()) {
            code.append(generateComponent(komponen, "        "));
        }
        
        code.append("""
            }
            """);
        
        return code.toString();
    }

    private String generateHomeScreen(Layar screen) {
        StringBuilder code = new StringBuilder();
        code.append("""
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Home") },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            ) { paddingValues ->
            """);
        
        // Check if there's a data source for products
        boolean hasProducts = screen.komponen().stream()
            .anyMatch(k -> k.dataSource() != null && k.dataSource().contains("product"));
        
        if (hasProducts) {
            code.append("""
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.products) { product ->
                        ProductCard(
                            product = product,
                            onClick = { navigator.navigate(Screen.ProductDetail(product.id)) }
                        )
                    }
                    
                    if (uiState.isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
                """);
        } else {
            code.append("""
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                """);
            for (Komponen komponen : screen.komponen()) {
                code.append(generateComponent(komponen, "    "));
            }
            code.append("""
                }
                """);
        }
        
        code.append("}\n");
        
        return code.toString();
    }

    private String generateProductDetailScreen(Layar screen) {
        return """
            val productId = navigator.currentRoute?.productId
            
            LaunchedEffect(productId) {
                productId?.let { viewModel.loadProduct(it) }
            }
            
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Product Detail") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (uiState.currentProduct != null) {
                        val product = uiState.currentProduct!!
                        
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Text(
                            text = "Rp ${product.price}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { viewModel.addToCart(product) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text("Add to Cart")
                        }
                    } else if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            """;
    }

    private String generateCartScreen(Layar screen) {
        return """
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Shopping Cart") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    if (uiState.cartItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Your cart is empty")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.cartItems) { cartItem ->
                                CartItemRow(
                                    cartItem = cartItem,
                                    onRemoveClick = { viewModel.removeFromCart(cartItem.productId) }
                                )
                            }
                        }
                        
                        Divider()
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Rp ${uiState.totalPrice}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Button(
                            onClick = { navigator.navigate(Screen.Checkout) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Checkout")
                        }
                    }
                }
            }
            """;
    }

    private String generateGenericScreen(Layar screen) {
        StringBuilder code = new StringBuilder();
        code.append("""
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
            """);
        
        for (Komponen komponen : screen.komponen()) {
            code.append(generateComponent(komponen, "    "));
        }
        
        code.append("}\n");
        return code.toString();
    }

    private String generateComponent(Komponen komponen, String indent) {
        StringBuilder code = new StringBuilder();
        
        switch (komponen.tipe()) {
            case TEXT -> {
                code.append(indent).append("Text(\n");
                code.append(indent).append("    text = \"").append(komponen.label() != null ? komponen.label() : "").append("\",\n");
                code.append(indent).append("    style = MaterialTheme.typography.bodyLarge\n");
                code.append(indent).append(")\n");
            }
            case BUTTON -> {
                code.append(indent).append("Button(\n");
                code.append(indent).append("    onClick = { ");
                if (komponen.actionType() == Komponen.ActionType.NAVIGATE) {
                    code.append("navigator.navigate(Screen.Home)");
                } else if (komponen.actionType() == Komponen.ActionType.SHOW_AD) {
                    code.append("viewModel.showInterstitialAd()");
                } else if (komponen.actionType() == Komponen.ActionType.SUBMIT_FORM) {
                    code.append("viewModel.submitForm()");
                } else {
                    code.append("// TODO: Implement action");
                }
                code.append(" }\n");
                code.append(indent).append(") {\n");
                code.append(indent).append("    Text(\"").append(komponen.label() != null ? komponen.label() : "Button").append("\")\n");
                code.append(indent).append(")\n");
            }
            case TEXTFIELD -> {
                code.append(indent).append("TextField(\n");
                code.append(indent).append("    value = uiState.").append(komponen.id()).append(",\n");
                code.append(indent).append("    onValueChange = { viewModel.update").append(capitalize(komponen.id())).append("(it) },\n");
                code.append(indent).append("    label = { Text(\"").append(komponen.label() != null ? komponen.label() : "").append("\") },\n");
                code.append(indent).append("    modifier = Modifier.fillMaxWidth()\n");
                code.append(indent).append(")\n");
            }
            case SPACER -> {
                code.append(indent).append("Spacer(modifier = Modifier.height(16.dp))\n");
            }
            case DIVIDER -> {
                code.append(indent).append("Divider()\n");
            }
            default -> {
                code.append(indent).append("// TODO: Implement ").append(komponen.tipe()).append("\n");
            }
        }
        
        return code.toString();
    }

    @Override
    public String generateViewModel(String screenName, Aplikasi aplikasi) {
        StringBuilder code = new StringBuilder();
        
        code.append("@ViewModelScoped\n");
        code.append("@HiltViewModel\n");
        code.append("class ").append(screenName).append("ViewModel @Inject constructor(\n");
        code.append("    private val repository: AppRepository,\n");
        code.append("    private val adManager: AdManager\n");
        code.append(") : ViewModel() {\n");
        code.append("\n");
        code.append("    data class UiState(\n");
        code.append("        val isLoading: Boolean = false,\n");
        code.append("        val products: List<Product> = emptyList(),\n");
        code.append("        val cartItems: List<CartItem> = emptyList(),\n");
        code.append("        val currentProduct: Product? = null,\n");
        code.append("        val totalPrice: String = \"0\",\n");
        code.append("        val email: String = \"\",\n");
        code.append("        val password: String = \"\"\n");
        code.append("    )\n");
        code.append("\n");
        code.append("    private val _uiState = MutableStateFlow(UiState())\n");
        code.append("    val uiState: StateFlow<UiState> = _uiState.asStateFlow()\n");
        code.append("\n");
        
        // Add Supabase integration if database is configured
        if (aplikasi.database() != null && aplikasi.database().provider() == DatabaseConfig.Provider.SUPABASE) {
            code.append("    init {\n");
            code.append("        loadProducts()\n");
            code.append("    }\n");
            code.append("\n");
            code.append("    fun loadProducts() {\n");
            code.append("        viewModelScope.launch {\n");
            code.append("            _uiState.update { it.copy(isLoading = true) }\n");
            code.append("            try {\n");
            code.append("                val products = repository.getProducts()\n");
            code.append("                _uiState.update { it.copy(products = products, isLoading = false) }\n");
            code.append("            } catch (e: Exception) {\n");
            code.append("                _uiState.update { it.copy(isLoading = false) }\n");
            code.append("            }\n");
            code.append("        }\n");
            code.append("    }\n");
            code.append("\n");
        }
        
        // Add ad-related methods if monetization is enabled
        if (aplikasi.monetization() != null && aplikasi.monetization().enabled()) {
            code.append("    fun showInterstitialAd() {\n");
            code.append("        adManager.showInterstitialAd()\n");
            code.append("    }\n");
            code.append("\n");
            code.append("    fun loadBannerAd() {\n");
            code.append("        adManager.loadBannerAd()\n");
            code.append("    }\n");
            code.append("\n");
        }
        
        // Add cart operations
        code.append("    fun addToCart(product: Product) {\n");
        code.append("        viewModelScope.launch {\n");
        code.append("            repository.addToCart(product)\n");
        code.append("            updateCartState()\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("\n");
        code.append("    fun removeFromCart(productId: String) {\n");
        code.append("        viewModelScope.launch {\n");
        code.append("            repository.removeFromCart(productId)\n");
        code.append("            updateCartState()\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("\n");
        code.append("    private fun updateCartState() {\n");
        code.append("        viewModelScope.launch {\n");
        code.append("            val cartItems = repository.getCartItems()\n");
        code.append("            val total = cartItems.sumOf { it.price * it.quantity }\n");
        code.append("            _uiState.update { it.copy(cartItems = cartItems, totalPrice = total.toString()) }\n");
        code.append("        }\n");
        code.append("    }\n");
        
        code.append("}\n");
        
        return code.toString();
    }

    private String generateMainActivity(Aplikasi aplikasi) {
        return """
            package %s
            
            import android.os.Bundle
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.setContent
            import androidx.activity.enableEdgeToEdge
            import androidx.compose.foundation.layout.fillMaxSize
            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.Surface
            import androidx.compose.ui.Modifier
            import dagger.hilt.android.AndroidEntryPoint
            
            @AndroidEntryPoint
            class MainActivity : ComponentActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    enableEdgeToEdge()
                    setContent {
                        MaterialTheme {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                GenAppNavigation()
                            }
                        }
                    }
                }
            }
            """.formatted(aplikasi.packageName());
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    @Override
    public String getFileExtension() {
        return ".kt";
    }

    @Override
    public String getMainFileName() {
        return "MainActivity";
    }
}
