package gg.optic.client.mixin;

import net.minecraft.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * Server SRV lookup (_minecraft._tcp.host) that also works on Java 9+.
 *
 * 1.7 loads com.sun.jndi.dns.DnsContextFactory through Minecraft's class loader, which only
 * sees Java's boot modules. On Java 9+ the DNS provider lives in jdk.naming.dns (a platform
 * module), so the lookup silently failed and e.g. minemen.club connected to its website's
 * address and timed out. Here the lookup runs with the normal Java class loader instead.
 */
@Mixin(ServerAddress.class)
public abstract class ServerAddressMixin {
	@Inject(method = "resolveSrv(Ljava/lang/String;)[Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
	private static void optic$resolveSrv(String host, CallbackInfoReturnable<String[]> cir) {
		Thread thread = Thread.currentThread();
		ClassLoader previous = thread.getContextClassLoader();
		try {
			thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
			env.put("java.naming.provider.url", "dns:");
			env.put("com.sun.jndi.dns.timeout.retries", "1");
			Attributes attributes = new InitialDirContext(env).getAttributes("_minecraft._tcp." + host, new String[] { "SRV" });
			Attribute srv = attributes.get("srv");
			if (srv != null && srv.size() > 0) {
				// "priority weight port target."
				String[] parts = srv.get(0).toString().split(" ", 4);
				if (parts.length == 4) {
					String target = parts[3].endsWith(".") ? parts[3].substring(0, parts[3].length() - 1) : parts[3];
					cir.setReturnValue(new String[] { target, parts[2] });
					return;
				}
			}
			cir.setReturnValue(new String[] { host, Integer.toString(25565) });
		} catch (Throwable t) {
			// No SRV record (or no DNS): plain host on the default port, like vanilla.
			cir.setReturnValue(new String[] { host, Integer.toString(25565) });
		} finally {
			thread.setContextClassLoader(previous);
		}
	}
}
