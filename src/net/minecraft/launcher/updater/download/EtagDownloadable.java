package net.minecraft.launcher.updater.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

public class EtagDownloadable extends Downloadable {

	public EtagDownloadable(Proxy proxy, URL remoteFile, File localFile,
			boolean forceDownload) {
		super(proxy, remoteFile, localFile, forceDownload);
	}

	@Override
	public String download() throws IOException {
		String localMd5 = null;

		if ((this.getTarget().getParentFile() != null) && (!this.getTarget().getParentFile().isDirectory())) {
			this.getTarget().getParentFile().mkdirs();
		}
		if ((!this.shouldIgnoreLocal()) && (this.getTarget().isFile())) {
			localMd5 = getDigest(this.getTarget(), "MD5", 65535);
		}

		if ((this.getTarget().isFile()) && (!this.getTarget().canWrite())) {
			throw new RuntimeException("Do not have write permissions for " + this.getTarget() + " - aborting!");
		}
		try {
			HttpURLConnection connection = this.makeConnection(this.getUrl());
			int status = connection.getResponseCode();

			if (status == 304) {
				return "Used own copy as it matched etag";
			}
			if (status / 100 == 2) {
				if (this.getExpectedSize() == 0L) {
					this.getMonitor().setTotal(connection.getContentLength());
				} else {
					this.getMonitor().setTotal(this.getExpectedSize());
				}

				InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), this.getMonitor());
				FileOutputStream outputStream = new FileOutputStream(this.getTarget());
				String md5 = copyAndDigest(inputStream, outputStream, "SHA-1", 65535);
				String etag = getEtag(connection);

				if (etag.contains("-")) {
					return "Didn't have etag so assuming our copy is good";
				}
				if (etag.equalsIgnoreCase(md5)) {
					return "Downloaded successfully and etag matched";
				}
				throw new RuntimeException(String.format(
						"E-tag did not match downloaded MD5 (ETag was %s, downloaded %s)", new Object[] { etag, md5 }));
			}
			if (this.getTarget().isFile()) {
				return "Couldn't connect to server (responded with " + status
						+ ") but have local file, assuming it's good";
			}
			throw new RuntimeException("Server responded with " + status);
		} catch (IOException e) {
			if (this.getTarget().isFile()) {
				return "Couldn't connect to server (" + e.getClass().getSimpleName() + ": '" + e.getMessage()
						+ "') but have local file, assuming it's good";
			}
			throw e;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Missing Digest.MD5", e);
		}
	}
	
	public static String getEtag(HttpURLConnection connection) {
		return getEtag(connection.getHeaderField("ETag"));
	}
	
	public static String getEtag(String etag) {
		if (etag == null) {
			etag = "-";
		} else if ((etag.startsWith("\"")) && (etag.endsWith("\""))) {
			etag = etag.substring(1, etag.length() - 1);
		}

		return etag;
	}

}
