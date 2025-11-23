package com.devhub.ocr.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Path;

/**
 * Simple Playwright runner that can be executed as a standalone Java program.
 * Usage: java -cp <classpath> com.devhub.ocr.playwright.PlaywrightRunner [url]
 * Or via Maven: ./mvnw -f backends/devhubocr exec:java -Dexec.mainClass=com.devhub.ocr.playwright.PlaywrightRunner -Dexec.args="https://example.com"
 */
public class PlaywrightRunner {
    public static void main(String[] args) {
        String url = args != null && args.length > 0 ? args[0] : "https://google.com";
        String outFile = args != null && args.length > 1 ? args[1] : "playwright-screenshot.png";

        System.out.println("PlaywrightRunner starting — navigating to: " + url);

        try (Playwright playwright = Playwright.create()) {
            BrowserType chromium = playwright.chromium();
            LaunchOptions options = new LaunchOptions();
            options.setHeadless(false);
            Browser browser = chromium.launch(options);
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.navigate(url);

            Path output = Path.of(outFile);
            page.screenshot(new Page.ScreenshotOptions().setPath(output));

            System.out.println("Saved screenshot to: " + output.toAbsolutePath());

            browser.close();
        } catch (Throwable t) {
            System.err.println("PlaywrightRunner failed: " + t.getMessage());
            t.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
