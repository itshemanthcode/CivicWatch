package com.example.claudeapp.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.claudeapp.data.imgbb.ImgBBRepository
import com.example.claudeapp.data.mapbox.MapboxConfig
import com.example.claudeapp.data.mapbox.MapboxRepository
import com.example.claudeapp.utils.ImageUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    
    @Provides
    @Singleton
    fun provideImgBBRepository(
        imageUtils: ImageUtils
    ): ImgBBRepository = ImgBBRepository(imageUtils)
    
    @Provides
    @Singleton
    fun provideImageUtils(): ImageUtils = ImageUtils()
    
    @Provides
    @Singleton
    fun provideMapboxConfig(): MapboxConfig = MapboxConfig()
    
    @Provides
    @Singleton
    fun provideMapboxRepository(): MapboxRepository = MapboxRepository()
    
    @Provides
    @Singleton
    fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.claudeapp.R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }
}

