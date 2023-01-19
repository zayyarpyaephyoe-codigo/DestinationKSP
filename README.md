# DestinationKSP


Usage
<code>
@Destination(name = "android_sample")
data class Sample(
    val name: String,
    val age: Int,
    val address: String,
    val isAndroid: Boolean
)
</code>


will generate to 


public object SampleDestination {
  public val NAME: String = "name"

  public val AGE: String = "age"

  public val ADDRESS: String = "address"

  public val ISANDROID: String = "isAndroid"

  public fun destination(): String = "android_sample/{name}/{age}/{address}/{isAndroid}"

  public fun route(
    name: String,
    age: Int,
    address: String,
    isAndroid: Boolean
  ): String = "android_sample/${name}/${age}/${address}/${isAndroid}"

  public fun name(bundle: Bundle): String? = bundle.getString("name")

  public fun age(bundle: Bundle): Int? = bundle.getInt("age")

  public fun address(bundle: Bundle): String? = bundle.getString("address")

  public fun isAndroid(bundle: Bundle): Boolean? = bundle.getBoolean("isAndroid")

  private fun String.encodeUrl(): String {
    if(this == "") {
      return ""
    }
    return URLEncoder.encode(this , StandardCharsets.UTF_8.toString())
  }
}
