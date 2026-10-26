package ip.logistics.shipping.parcel.application.command

import net.bytebuddy.ByteBuddy
import java.util.concurrent.atomic.AtomicInteger

class Hyp001 {
    private val loaded = mutableListOf<Class<*>>()
    private val counter = AtomicInteger()

    fun doWorkSilentlySoft() {
        try {
            val id = counter.incrementAndGet()
            val className = "leak.gen.Generated$id"

            val dynamicType = ByteBuddy()
                .subclass(Any::class.java)
                .name(className)
                .make()

            val loadedClass = dynamicType
                .load(this::class.java.classLoader)
                .loaded

            // keep reference → class IS NOT unloaded
            loaded += loadedClass

        } catch (t: Throwable) {
            // silently swallow all errors
        }
    }

    fun doWorkSilentlyHard() {
        try {
            // attempt to invoke non-existent method -> NoSuchMethodException
            val cls = String::class.java
            val m = cls.getMethod("definitelyMissingMethod") // exception
            m.invoke("abc")
//            cls.equals()

        } catch (ex: Exception) {
            // HYPOTHESIS H-0002 — silent swallowing
            // no logging
        }
    }
}