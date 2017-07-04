import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.stringType

object database : PropertyGroup() {
    val username by stringType
    val password by stringType
}
