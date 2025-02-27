import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.html.*
import kotlinx.html.stream.createHTML

@Serializable
data class Meteo(
    val listDatosDiarios: List<DatosDiarios>
)

@Serializable
data class DatosDiarios(
    val `data`: String,
    val listaEstacions: List<ListaEstacions>
)

@Serializable
data class ListaEstacions(
    val concello: String,
    val estacion: String,
    val idEstacion: Int,
    val listaMedidas: List<ListaMedidas>,
    val provincia: String,
    val utmx: String,
    val utmy: String
)

@Serializable
data class ListaMedidas(
    val codigoParametro: String,
    val lnCodigoValidacion: Int,
    val nomeParametro: String,
    val unidade: String,
    val valor: Double
)

data class temperaturaMaxima(
    val estacion: String,
    val temperatura: Double
)
fun main() {

    //  crear cliente http
    val client = HttpClient.newHttpClient()

    // crear solicitud
    val request = HttpRequest.newBuilder()
        .uri(URI.create("https://servizos.meteogalicia.gal/mgrss/observacion/datosDiariosEstacionsMeteo.action"))
        .GET()
        .build()

    //  Enviar la solicitud con el cliente
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())

    // obtener string con datos
    val jsonBody = response.body()

    // Deserializar el JSON a una lista de objetos User
    val meteo: List<Meteo>
    if (jsonBody[0] == '{') {
        val temporal = mutableListOf<Meteo>()
        temporal.add(Json.decodeFromString(jsonBody))
        meteo = temporal.toList()
    }
    else {
        meteo = Json.decodeFromString(jsonBody)
    }


    //println(users)

    // Imprimir los usuarios con diversos campos
    val paginaweb = createHTML().html {
        lang = "es"
        head {
            meta(charset = "UTF-8")
            title("Datos meteoroligocos")
        }
        body {
            h1 { +"#1" }
            h2 { +"Concellos de A Coruña con estacion ordenados alfabeticamente" }
            meteo.forEach { meteo ->
                meteo.listDatosDiarios.forEach{
                    ul {
                        it.listaEstacions.filter { it.provincia == "A Coruña" }.distinctBy { it.concello }
                            .sortedBy { it.concello }.forEach {
                            li { +"Concello: ${it.concello}" }
                        }
                    }
                }
            }
            p {  }
            h1 { +"#2" }
            h2 { +"Estaciones mayor temperatura maxima a menor" }
            table { style = "border: 1px solid black;"
                tr {
                    th { style = "border: 1px solid black;"; +"estacion" }
                    th { style = "border: 1px solid black;"; +"temperatura" }
                }
                meteo.forEach { meteo ->
                    meteo.listDatosDiarios.forEach{
                        val listaTemperaturas = it.listaEstacions.map { estacion ->
                            val temperatura = estacion.listaMedidas.find { it.codigoParametro == "TA_MAX_1.5m" }?.valor ?: -9999.0 // Si el valor es nulo indicamos -9999.0 viendo los numeros de algunas estaciones
                            temperaturaMaxima(estacion.estacion, temperatura)
                        }
                        listaTemperaturas.sortedByDescending { it.temperatura }.forEach{
                            tr {
                                td { style = "text-align: center; border: 1px solid black;" ; +it.estacion } // atributos dado por copilot
                                td { style = "text-align: center; border: 1px solid black;" ; +it.temperatura.toString() } // atributos dado por copilot
                            }
                        }
                    }
                }
            }


            p {  }
            h1 { + "#3"}
            h2 { +"Numero de estaciones por provincia" }
            table { style = "border: 1px solid black"
                tr {
                    th { style = "text-align: center; border: 1px solid black;" ; +"provincia" }
                    th { style = "text-align: center; border: 1px solid black;" ; +"numero estaciones"}
                }
                meteo.forEach { meteo ->
                    meteo.listDatosDiarios.forEach {

                            it.listaEstacions.groupBy { it.provincia }.forEach {
                                tr {
                                    td { style = "text-align: center; border: 1px solid black;" ; +it.key }
                                    td { style = "text-align: center; border: 1px solid black;" ; +it.value.size.toString()}
                                }
                            }

                    }
                }
            }
        }
    }
    File("test.html").writeText(paginaweb)
}
