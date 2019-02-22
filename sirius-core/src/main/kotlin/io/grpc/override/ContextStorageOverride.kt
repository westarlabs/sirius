package io.grpc.override

import io.grpc.Context
import org.starcoin.sirius.util.WithLogging
import java.util.logging.Level


/**
 * A default implementation of grpc-context's Storage
 * See https://grpc.io/grpc-java/javadoc/io/grpc/Context.Storage.html for
 * the reason why this class must exist at `io.grpc.override.ContextStorageOverride`.
 */
class ContextStorageOverride : Context.Storage() {

    //TODO implements a custom ContextStorageOverride

    override fun doAttach(toAttach: Context): Context? {
        val current = current()
        localContext.set(toAttach)
        return current
    }

    override fun detach(toDetach: Context, toRestore: Context) {
        if (current() !== toDetach) {
            // Log a severe message instead of throwing an exception as the context to attach is assumed
            // to be the correct one and the unbalanced state represents a coding mistake in a lower
            // layer in the stack that cannot be recovered from here.
            LOG.log(
                Level.SEVERE, "Context was not attached when detaching",
                Throwable().fillInStackTrace()
            )
        }
        doAttach(toRestore)
    }

    override fun current(): Context? {
        return localContext.get()
    }

    companion object : WithLogging() {
        /**
         * Currently bound context.
         */
        private val localContext = ThreadLocal<Context>()
    }
}
