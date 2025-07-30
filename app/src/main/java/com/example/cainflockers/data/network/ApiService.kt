package com.example.cainflockers.data.network

import com.example.cainflockers.data.models.Gsx2JsonResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api")
    suspend fun getSolicitudes(
        @Query("id") sheetId: String = "2PACX-1vSXWcbMwf9FPU4PId68Znb3sMl9aVBI57K9VkZtu-q_RugNOb2wbL939ARsmo50BnFp12J1r_CFw0fj",
        @Query("sheet") sheetName: String = "Solicitudes"
    ): Gsx2JsonResponse
}

