package net.minecraft.launcher.updater.download;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class Downloadable {
	private final URL url;
	private final File target;
	private final boolean forceDownload;
	private final Proxy proxy;
	private final ProgressContainer monitor;
	protected int numAttempts;
	private long expectedSize;

	public Downloadable(Proxy proxy, URL remoteFile, File localFile, boolean forceDownload) {
		this.proxy = proxy;
		this.url = remoteFile;
		this.target = localFile;
		this.forceDownload = forceDownload;
		this.monitor = new ProgressContainer();
	}

	public ProgressContainer getMonitor() {
		return this.monitor;
	}

	public long getExpectedSize() {
		return this.expectedSize;
	}

	public void setExpectedSize(long expectedSize) {
		this.expectedSize = expectedSize;
	}
	
	public void ensureFileWritable()
	{

	}
	
	protected void updateExpectedSize(HttpURLConnection u)
	{
		
	}

	public abstract String download() throws IOException;

	protected HttpURLConnection makeConnection(URL connecturl) throws IOException {
		if (connecturl.getProtocol().equals("https")) {
			connecturl = new URL("http", connecturl.getHost(), connecturl.getPort(), connecturl.getFile());
		}
		HttpURLConnection connection = (HttpURLConnection) connecturl.openConnection(this.proxy);

		connection.setUseCaches(false);
		connection.setDefaultUseCaches(false);
		connection.setRequestProperty("Cache-Control", "no-store,max-age=0,no-cache");
		connection.setRequestProperty("Expires", "0");
		connection.setRequestProperty("Pragma", "no-cache");

//		connection.connect();

		return connection;
	}

	public URL getUrl() {
		return this.url;
	}

	public File getTarget() {
		return this.target;
	}

	public boolean shouldIgnoreLocal() {
		return this.forceDownload;
	}

	public int getNumAttempts() {
		return this.numAttempts;
	}

	public Proxy getProxy() {
		return this.proxy;
	}

	public static String getDigest(File digestFile, String method, int buffersize) {
		DigestInputStream stream = null;
		try {
			stream = new DigestInputStream(new FileInputStream(digestFile), MessageDigest.getInstance(method));
			byte[] buffer = new byte[buffersize];
			
			int read = 0;
			read = stream.read(buffer);
			while (read >= 1) {
				read = stream.read(buffer);
			}
		} catch (Exception ignored) {
			return null;
		} finally {
			closeSilently(stream);
		}
		String s = String.format("%1$032x", new Object[] { new BigInteger(1, stream.getMessageDigest().digest()) });
		System.out.println(s);
		return s;
	}

	public static void closeSilently(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException localIOException) {
			}
		}
	}

	public static String copyAndDigest(InputStream inputStream, OutputStream outputStream, String method, int buffersize) throws IOException,
			NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(method);
		byte[] buffer = new byte[buffersize];
		try {
			int read = inputStream.read(buffer);
			while (read >= 1) {
				digest.update(buffer, 0, read);
				outputStream.write(buffer, 0, read);
				read = inputStream.read(buffer);
			}
		} finally {
			closeSilently(inputStream);
			closeSilently(outputStream);
		}
		String s = String.format("%1$032x", new Object[] { new BigInteger(1, digest.digest()) });
		System.out.println("buf:" + s);
		return s;
	}
}