package com.example.connectivitymanagermodule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import java.io.IOException
import java.net.*
import java.util.concurrent.*
import javax.net.SocketFactory


/** check an Internet connection.
 * @author javad shirkhani
 * @version 1.0
 */
object CheckConnectivityModule {


    private var hasInternet: Boolean = true
    private var hasConnection: Boolean = true

    /**
     * This method have to use in application class.
     * Don't forget to initialize class using this method
     * @param context use application context if you want to observe Internet connection in whole application
     */
    fun initialize(context: Context) {
        val connectionLiveData = ConnectionLiveData(context)
        connectionLiveData.observeForever {
            Log.d("CheckConnectivityModule", "hasConnection :  $it")
            hasConnection = it
            hasInternet = if (it) {
                runCommand() && internetConnectionAvailable(1000)
            } else {
                false
            }
            Log.d("CheckConnectivityModule", "hasInternet :  $hasInternet")
        }
    }


    /**
     * This method return Internet connection state.
     * @return This returns connectivity state.
     * The return type is enum class
     */
    fun checkHasConnectionAndInternet(): ConnectivityState {
        return if (!hasConnection) {
            ConnectivityState.NOCONNECTION
        } else {
            if (hasInternet)
                ConnectivityState.HASINTERNET
            else
                ConnectivityState.NOINTERNET
        }
    }

    class ConnectionLiveData(val context: Context) : LiveData<Boolean>() {

        var intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        private var connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        private lateinit var networkCallback: NetworkCallback

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                networkCallback = NetworkCallback(this)
            }
        }

        override fun onActive() {
            super.onActive()
            updateConnection()
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> connectivityManager.registerDefaultNetworkCallback(
                    networkCallback
                )
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    val builder = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).addTransportType(
                            NetworkCapabilities.TRANSPORT_WIFI
                        )
                    connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
                }
                else -> {
                    context.registerReceiver(networkReceiver, intentFilter)
                }
            }
        }

        override fun onInactive() {
            super.onInactive()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            } else {
                context.unregisterReceiver(networkReceiver)
            }
        }


        private val networkReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateConnection()
            }
        }

        fun updateConnection() {
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            postValue(activeNetwork?.isConnectedOrConnecting == true)
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        class NetworkCallback(val liveData: ConnectionLiveData) :
            ConnectivityManager.NetworkCallback() {


            override fun onAvailable(network: Network) {
                liveData.postValue(true)
            }

            override fun onLost(network: Network) {
                liveData.postValue(false)
            }
        }
    }

    private fun runCommand(): Boolean {
        val runtime = Runtime.getRuntime();
        return try {
            val mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8")
            val mExitValue = mIpAddrProcess.waitFor()
            mExitValue == 0
        } catch (e: Exception) {
            hasConnection
        }
    }

    fun isInternetAvailable(): Boolean {
        return try {
            val address: InetAddress = InetAddress.getByName("google.com")
            //You can replace it with your name
            !address.equals("")
        } catch (e: java.lang.Exception) {
            false
        }
    }

    enum class ConnectivityState {
        NOCONNECTION, HASINTERNET, NOINTERNET
    }

    private fun execute(socketFactory: SocketFactory): Boolean {
        return try {
            val socket = socketFactory.createSocket() ?: throw IOException("Socket is null.")
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
            socket.close()
            true
        } catch (e: IOException) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkHasInternet(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    fun isConnected(): Boolean {
        return try {
            val url = URL("http://www.google.com/")
            val urlc: HttpURLConnection = url.openConnection() as HttpURLConnection
            urlc.setRequestProperty("User-Agent", "test")
            urlc.setRequestProperty("Connection", "close")
            urlc.setConnectTimeout(1000) // mTimeout is in seconds
            urlc.connect()
            urlc.getResponseCode() === 200
        } catch (e: IOException) {
            Log.i("CheckConnectivityModule", "Error checking internet connection", e)
            false
        }

    }

    private fun internetConnectionAvailable(timeOut: Int): Boolean {
        var inetAddress: InetAddress? = null
        try {
            val future: Future<InetAddress?>? =
                Executors.newSingleThreadExecutor().submit(object : Callable<InetAddress?> {
                    override fun call(): InetAddress? {
                        return try {
                            InetAddress.getByName("radar.arvancloud.com")
                        } catch (e: UnknownHostException) {
                            null
                        }
                    }
                })
            if (future != null) {
                inetAddress = future.get(timeOut.toLong(), TimeUnit.MILLISECONDS)
            }
            if (future != null) {
                future.cancel(true)
            }
        } catch (e: InterruptedException) {
        } catch (e: ExecutionException) {
        } catch (e: TimeoutException) {
        }
        return inetAddress != null && !inetAddress.equals("")
    }
}
