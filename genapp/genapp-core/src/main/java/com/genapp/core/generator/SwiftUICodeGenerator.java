package com.genapp.core.generator;

import com.genapp.core.model.*;
import org.springframework.stereotype.Component;

/**
 * Code Generator untuk iOS SwiftUI (Swift)
 */
@Component
public class SwiftUICodeGenerator implements CodeGenerator {

    @Override
    public String generateAppCode(Aplikasi aplikasi) {
        StringBuilder code = new StringBuilder();
        
        // Generate ContentView (main entry point)
        code.append(generateContentView(aplikasi));
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
        
        code.append("struct ").append(screen.nama()).append("View: View {\n");
        code.append("    @StateObject private var viewModel = ").append(screen.nama()).append("ViewModel()\n");
        code.append("    @Environment(\\.navigation) private var navigation\n");
        code.append("    \n");
        
        // Add monetization code if enabled
        if (aplikasi.monetization() != null && aplikasi.monetization().enabled()) {
            code.append("    // AdMob Banner Ad\n");
            code.append("    @State private var showBannerAd = true\n");
            code.append("    \n");
        }
        
        code.append("    var body: some View {\n");
        
        // Generate screen content based on type
        code.append(generateScreenContent(screen, aplikasi));
        
        code.append("    }\n");
        code.append("}\n");
        
        return code.toString();
    }

    private String generateScreenContent(Layar screen, Aplikasi aplikasi) {
        StringBuilder code = new StringBuilder();
        
        switch (screen.tipe()) {
            case SPLASH:
                code.append(generateSplashScreen());
                break;
            case LOGIN:
                code.append(generateLoginScreen(screen));
                break;
            case HOME:
                code.append(generateHomeScreen(screen, aplikasi));
                break;
            case PRODUCT_DETAIL:
                code.append(generateProductDetailScreen());
                break;
            case CART:
                code.append(generateCartScreen());
                break;
            default:
                code.append(generateGenericScreen(screen));
        }
        
        return code.toString();
    }

    private String generateSplashScreen() {
        return """
            VStack(spacing: 20) {
                Text("GenApp")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.blue)
                
                ProgressView()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.white)
            .onAppear {
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    navigation.navigate(to: "home")
                }
            }
            """;
    }

    private String generateLoginScreen(Layar screen) {
        StringBuilder code = new StringBuilder();
        code.append("""
            VStack(spacing: 16) {
                Text("Login")
                    .font(.title)
                    .fontWeight(.bold)
                
                Spacer()
                    .frame(height: 32)
            """);
        
        // Add components from screen definition
        for (Komponen komponen : screen.komponen()) {
            code.append(generateComponent(komponen));
        }
        
        code.append("""
            }
            .padding(24)
            """);
        
        return code.toString();
    }

    private String generateHomeScreen(Layar screen, Aplikasi aplikasi) {
        StringBuilder code = new StringBuilder();
        code.append("""
            NavigationView {
                VStack {
            """);
        
        // Check if there's a data source for products
        boolean hasProducts = screen.komponen().stream()
            .anyMatch(k -> k.dataSource() != null && k.dataSource().contains("product"));
        
        if (hasProducts) {
            code.append("""
                    List(viewModel.products) { product in
                        Button(action: {
                            navigation.navigate(to: "product/\\(product.id)")
                        }) {
                            ProductRow(product: product)
                        }
                    }
                    
                    if viewModel.isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    }
                """);
        } else {
            code.append("""
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                """);
            for (Komponen komponen : screen.komponen()) {
                code.append(generateComponent(komponen));
            }
            code.append("""
                        }
                        .padding()
                    }
                """);
        }
        
        code.append("""
                }
                .navigationTitle("Home")
            }
            """);
        
        return code.toString();
    }

    private String generateProductDetailScreen() {
        return """
            NavigationView {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        if let product = viewModel.currentProduct {
                            AsyncImage(url: URL(string: product.imageUrl)) { image in
                                image.resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(height: 300)
                            } placeholder: {
                                ProgressView()
                            }
                            
                            Text(product.name)
                                .font(.title)
                                .fontWeight(.bold)
                            
                            Text("Rp \\(product.price)")
                                .font(.title2)
                                .foregroundColor(.blue)
                            
                            Text(product.description)
                                .font(.body)
                            
                            Button(action: {
                                viewModel.addToCart(product: product)
                            }) {
                                Text("Add to Cart")
                                    .frame(maxWidth: .infinity)
                                    .padding()
                                    .background(Color.blue)
                                    .foregroundColor(.white)
                                    .cornerRadius(10)
                            }
                        } else if viewModel.isLoading {
                            ProgressView()
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                        }
                    }
                    .padding()
                }
                .navigationTitle("Product Detail")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(action: {
                            navigation.pop()
                        }) {
                            Image(systemName: "chevron.left")
                        }
                    }
                }
            }
            """;
    }

    private String generateCartScreen() {
        return """
            NavigationView {
                VStack {
                    if viewModel.cartItems.isEmpty {
                        VStack {
                            Image(systemName: "cart")
                                .font(.system(size: 50))
                                .foregroundColor(.gray)
                            Text("Your cart is empty")
                                .font(.headline)
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else {
                        List(viewModel.cartItems) { cartItem in
                            HStack {
                                Text(cartItem.productName)
                                Spacer()
                                Text("Rp \\(cartItem.price * cartItem.quantity)")
                                Button(action: {
                                    viewModel.removeFromCart(productId: cartItem.productId)
                                }) {
                                    Image(systemName: "trash")
                                        .foregroundColor(.red)
                                }
                            }
                        }
                        
                        Divider()
                        
                        HStack {
                            Text("Total:")
                                .font(.title2)
                                .fontWeight(.bold)
                            Spacer()
                            Text("Rp \\(viewModel.totalPrice)")
                                .font(.title2)
                                .fontWeight(.bold)
                                .foregroundColor(.blue)
                        }
                        .padding()
                        
                        NavigationLink(destination: CheckoutView()) {
                            Text("Checkout")
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                        .padding()
                    }
                }
                .navigationTitle("Shopping Cart")
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(action: {
                            navigation.pop()
                        }) {
                            Image(systemName: "chevron.left")
                        }
                    }
                }
            }
            """;
    }

    private String generateGenericScreen(Layar screen) {
        StringBuilder code = new StringBuilder();
        code.append("""
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
            """);
        
        for (Komponen komponen : screen.komponen()) {
            code.append(generateComponent(komponen));
        }
        
        code.append("""
                }
                .padding()
            }
            """);
        
        return code.toString();
    }

    private String generateComponent(Komponen komponen) {
        StringBuilder code = new StringBuilder();
        
        switch (komponen.tipe()) {
            case TEXT -> {
                code.append("                Text(\"").append(komponen.label() != null ? komponen.label() : "").append("\")\n");
            }
            case BUTTON -> {
                code.append("                Button(action: {\n");
                if (komponen.actionType() == Komponen.ActionType.NAVIGATE) {
                    code.append("                    navigation.navigate(to: \"home\")\n");
                } else if (komponen.actionType() == Komponen.ActionType.SHOW_AD) {
                    code.append("                    viewModel.showInterstitialAd()\n");
                } else if (komponen.actionType() == Komponen.ActionType.SUBMIT_FORM) {
                    code.append("                    viewModel.submitForm()\n");
                } else {
                    code.append("                    // TODO: Implement action\n");
                }
                code.append("                }) {\n");
                code.append("                    Text(\"").append(komponen.label() != null ? komponen.label() : "Button").append("\")\n");
                code.append("                        .frame(maxWidth: .infinity)\n");
                code.append("                        .padding()\n");
                code.append("                        .background(Color.blue)\n");
                code.append("                        .foregroundColor(.white)\n");
                code.append("                        .cornerRadius(10)\n");
                code.append("                }\n");
            }
            case TEXTFIELD -> {
                code.append("                TextField(\"").append(komponen.label() != null ? komponen.label() : "").append("\", text: $viewModel.").append(komponen.id()).append(")\n");
                code.append("                    .textFieldStyle(RoundedBorderTextFieldStyle())\n");
            }
            case SPACER -> {
                code.append("                Spacer()\n");
            }
            case DIVIDER -> {
                code.append("                Divider()\n");
            }
            default -> {
                code.append("                // TODO: Implement ").append(komponen.tipe()).append("\n");
            }
        }
        
        return code.toString();
    }

    @Override
    public String generateViewModel(String screenName, Aplikasi aplikasi) {
        StringBuilder code = new StringBuilder();
        
        code.append("@MainActor\nclass ").append(screenName).append("ViewModel: ObservableObject {\n");
        code.append("    @Published var isLoading = false\n");
        code.append("    @Published var products: [Product] = []\n");
        code.append("    @Published var cartItems: [CartItem] = []\n");
        code.append("    @Published var currentProduct: Product?\n");
        code.append("    @Published var totalPrice: String = \"0\"\n");
        code.append("    @Published var email = \"\"\n");
        code.append("    @Published var password = \"\"\n");
        code.append("    \n");
        
        // Add Supabase integration if database is configured
        if (aplikasi.database() != null && aplikasi.database().provider() == DatabaseConfig.Provider.SUPABASE) {
            code.append("    init() {\n");
            code.append("        loadProducts()\n");
            code.append("    }\n");
            code.append("    \n");
            code.append("    func loadProducts() {\n");
            code.append("        Task {\n");
            code.append("            isLoading = true\n");
            code.append("            do {\n");
            code.append("                let products = try await Repository.shared.getProducts()\n");
            code.append("                self.products = products\n");
            code.append("            } catch {\n");
            code.append("                print(\"Error loading products: \\(error)\")\n");
            code.append("            }\n");
            code.append("            isLoading = false\n");
            code.append("        }\n");
            code.append("    }\n");
            code.append("    \n");
        }
        
        // Add ad-related methods if monetization is enabled
        if (aplikasi.monetization() != null && aplikasi.monetization().enabled()) {
            code.append("    func showInterstitialAd() {\n");
            code.append("        AdManager.shared.showInterstitialAd()\n");
            code.append("    }\n");
            code.append("    \n");
            code.append("    func loadBannerAd() {\n");
            code.append("        AdManager.shared.loadBannerAd()\n");
            code.append("    }\n");
            code.append("    \n");
        }
        
        // Add cart operations
        code.append("    func addToCart(product: Product) {\n");
        code.append("        Task {\n");
        code.append("            try? await Repository.shared.addToCart(product)\n");
        code.append("            updateCartState()\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("    \n");
        code.append("    func removeFromCart(productId: String) {\n");
        code.append("        Task {\n");
        code.append("            try? await Repository.shared.removeFromCart(productId)\n");
        code.append("            updateCartState()\n");
        code.append("        }\n");
        code.append("    }\n");
        code.append("    \n");
        code.append("    private func updateCartState() {\n");
        code.append("        Task {\n");
        code.append("            cartItems = try? await Repository.shared.getCartItems()\n");
        code.append("            let total = cartItems.reduce(0) { $0 + ($1.price * $1.quantity) }\n");
        code.append("            totalPrice = String(total)\n");
        code.append("        }\n");
        code.append("    }\n");
        
        code.append("}\n");
        
        return code.toString();
    }

    private String generateContentView(Aplikasi aplikasi) {
        return """
            import SwiftUI
            
            @main
            struct %sApp: App {
                var body: some Scene {
                    WindowGroup {
                        ContentView()
                    }
                }
            }
            
            struct ContentView: View {
                var body: some View {
                    SplashScreen()
                }
            }
            """.formatted(aplikasi.nama().replaceAll("\\s+", ""));
    }

    @Override
    public String getFileExtension() {
        return ".swift";
    }

    @Override
    public String getMainFileName() {
        return "ContentView";
    }
}
