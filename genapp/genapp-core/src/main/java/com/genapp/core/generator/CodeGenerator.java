package com.genapp.core.generator;

import com.genapp.core.model.Aplikasi;

/**
 * Interface untuk code generator
 */
public interface CodeGenerator {
    
    /**
     * Generate kode untuk seluruh aplikasi
     */
    String generateAppCode(Aplikasi aplikasi);
    
    /**
     * Generate kode untuk layar tertentu
     */
    String generateScreenCode(Aplikasi aplikasi, int screenIndex);
    
    /**
     * Generate ViewModel untuk layar
     */
    String generateViewModel(String screenName, Aplikasi aplikasi);
    
    /**
     * Get file extension untuk platform target
     */
    String getFileExtension();
    
    /**
     * Get nama file utama (MainActivity / ContentView)
     */
    String getMainFileName();
}
