package com.lucas.predictaapp.data.remote

import com.lucas.predictaapp.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {
    val client: SupabaseClient? by lazy {
        val url = BuildConfig.SUPABASE_URL.takeIf { it.isNotBlank() } ?: return@lazy null
        val key = BuildConfig.SUPABASE_ANON_KEY.takeIf { it.isNotBlank() } ?: return@lazy null
        createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
            install(Postgrest)
        }
    }
}
