package net.minecraft.launcher.updater.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

public class ChecksummedDownloadable extends Downloadable {

	public ChecksummedDownloadable(Proxy proxy, URL remoteFile, File localFile,
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
			localMd5 = getDigest(this.getTarget(), "SHA-1", 65535);
		}

		String localshafile = this.getTarget().toString() + ".sha";
		File f = new File(localshafile);
		if(f.exists() && !f.isDirectory()) 
		{ 		
			String shafilecontents = readFile(localshafile, Charset.defaultCharset());
			if (localMd5 != null && shafilecontents != null)
			{
				if (localMd5.equals(shafilecontents))
				{
					return "Used own copy as it matched sha";
				}
			} 
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
				copyAndDigest(inputStream, outputStream, "SHA-1", 65535);
				return "Sucessfully downloaded.";
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
	
	private String readFile(String path, Charset encoding) 
			  throws IOException 
			{
			  byte[] encoded = Files.readAllBytes(Paths.get(path));
			  return encoding.decode(ByteBuffer.wrap(encoded)).toString();
			}

}
