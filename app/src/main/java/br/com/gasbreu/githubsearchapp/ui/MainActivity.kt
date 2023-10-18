package br.com.gasbreu.githubsearchapp.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import br.com.gasbreu.githubsearchapp.data.GitHubService
import br.com.gasbreu.githubsearchapp.domain.Repository
import br.com.gasbreu.githubsearchapp.ui.adapter.RepositoryAdapter
import com.example.githubsearch.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    lateinit var nomeUsuario: EditText
    lateinit var btnConfirmar: Button
    lateinit var listaRepositories: RecyclerView
    lateinit var githubApi: GitHubService
    lateinit var carregamento : ProgressBar
    lateinit var icWifiOff : ImageView
    lateinit var txtWifiOff : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayShowHomeEnabled(true)

        setContentView(R.layout.activity_main)
        setupView()
        showUserName()
        setupRetrofit()
        setupListeners()
    }

    private fun setupView() {
        nomeUsuario = findViewById(R.id.et_nome_usuario)
        btnConfirmar = findViewById(R.id.btn_pesquisar)
        listaRepositories = findViewById(R.id.rv_lista_repositories)
        carregamento = findViewById(R.id.pb_carregamento)
        icWifiOff = findViewById(R.id.iv_wifi_off)
        txtWifiOff = findViewById(R.id.tv_wifi_off)
    }

    private fun setupRetrofit() {

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        githubApi = retrofit.create(GitHubService::class.java)
    }

    private fun setupListeners() {
        btnConfirmar.setOnClickListener {

            val conexao = isInternetAvailable()

            if (!conexao) {
                icWifiOff.isVisible = true
                txtWifiOff.isVisible = true
            } else {

                icWifiOff.isVisible = false
                txtWifiOff.isVisible = false

                val nomePesquisar = nomeUsuario.text.toString()
                getAllReposByUserName(nomePesquisar)
                saveUserLocal()
                listaRepositories.isVisible = false
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun getAllReposByUserName(userName: String) {

        if (userName.isNotEmpty()) {

            carregamento.isVisible = true

            githubApi.getAllRepositoriesByUser(userName)
                .enqueue(object : Callback<List<Repository>> {

                    override fun onResponse(
                        call: Call<List<Repository>>,
                        response: Response<List<Repository>>
                    ) {
                        if (response.isSuccessful) {

                            carregamento.isVisible = false
                            listaRepositories.isVisible = true

                            val repositories = response.body()

                            repositories?.let {
                                setupAdapter(repositories)
                            }

                        } else {

                            carregamento.isVisible = false

                            val context = applicationContext
                            Toast.makeText(context, R.string.response_error, Toast.LENGTH_LONG)
                                .show()
                        }
                    }

                    override fun onFailure(call: Call<List<Repository>>, t: Throwable) {

                        carregamento.isVisible = false

                        val context = applicationContext
                        Toast.makeText(context, R.string.response_error, Toast.LENGTH_LONG).show()
                    }

                })
        }
    }

    fun setupAdapter(list: List<Repository>) {

        val adapter = RepositoryAdapter(
            this, list)

        listaRepositories.adapter = adapter
    }

    private fun saveUserLocal() {

        val usuarioInformado = nomeUsuario.text.toString()

        val sharedPreference = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPreference.edit()) {
            putString("saved_username", usuarioInformado)
            apply()
        }
    }

    private fun showUserName() {

        val sharedPreference = getPreferences(MODE_PRIVATE) ?: return
        val ultimoPesquisado = sharedPreference.getString("saved_username", null)

        if (!ultimoPesquisado.isNullOrEmpty()) {
            nomeUsuario.setText(ultimoPesquisado)
        }
    }

}