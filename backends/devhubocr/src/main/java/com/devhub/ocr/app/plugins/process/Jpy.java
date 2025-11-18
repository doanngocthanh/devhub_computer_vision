package com.devhub.ocr.app.plugins.process;

import java.io.*;
import java.util.Base64;

public class Jpy {
	private static OCRProcess ocrProcess;

	static {
		try {
			ocrProcess = new OCRProcess();
		} catch (IOException e) {
			throw new RuntimeException("Failed to start Python worker", e);
		}
	}

	public static String sendOCR(String task, byte[] imageBytes) throws IOException {
		return ocrProcess.sendOCR(task, imageBytes);
	}

	public static void close() throws IOException {
		ocrProcess.close();
	}
}

class OCRProcess {
	private Process process;
	private BufferedWriter writer;
	private BufferedReader reader;

	public OCRProcess() throws IOException {
		ProcessBuilder pb = new ProcessBuilder("python", "ocr_worker.py");
		pb.redirectErrorStream(true);
		process = pb.start();

		writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	}

	public String sendOCR(String task, byte[] imageBytes) throws IOException {
		String b64 = Base64.getEncoder().encodeToString(imageBytes);

		String json = String.format(
				"{\"task\":\"%s\", \"image\":\"%s\"}",
				task, b64
		);

		writer.write(json + "\n");
		writer.flush();

		String response = reader.readLine();
		return response;
	}

	public void close() throws IOException {
		writer.close();
		reader.close();
		process.destroy();
	}
}
