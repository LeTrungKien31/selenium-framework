// src/main/java/framework/base/BaseTest.java
package framework.base;

import framework.config.ConfigReader;
import framework.utils.ScreenshotUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public abstract class BaseTest {
    private static final ThreadLocal<WebDriver> tlDriver = new ThreadLocal<>();

    protected WebDriver getDriver() {
        return tlDriver.get();
    }

    @Parameters({"browser", "env"})
    @BeforeMethod(alwaysRun = true)
    public void setUp(@Optional("chrome") String browser,
                      @Optional("dev") String env) {
        System.setProperty("env", env);

        // GitHub Actions tự động set CI=true
        boolean isCI = System.getenv("CI") != null;

        // Kiểm tra có chạy trên Grid không
        String gridUrl = System.getProperty("grid.url");

        WebDriver driver;
        if (gridUrl != null && !gridUrl.isBlank()) {
            driver = createRemoteDriver(browser, gridUrl);
        } else {
            driver = createLocalDriver(browser, isCI);
        }

        driver.manage().window().maximize();
        driver.manage().timeouts()
              .implicitlyWait(Duration.ofSeconds(
                  ConfigReader.getInstance().getImplicitWait()));
        driver.get(ConfigReader.getInstance().getBaseUrl());
        tlDriver.set(driver);
    }

    private WebDriver createLocalDriver(String browser, boolean headless) {
        switch (browser.toLowerCase()) {
            case "firefox": {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("-headless");
                    System.out.println("[Driver] Firefox HEADLESS (CI)");
                }
                return new FirefoxDriver(options);
            }
            default: {
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                    options.addArguments("--no-sandbox");
                    options.addArguments("--disable-dev-shm-usage");
                    options.addArguments("--window-size=1920,1080");
                    options.addArguments("--disable-gpu");
                    System.out.println("[Driver] Chrome HEADLESS (CI)");
                } else {
                    System.out.println("[Driver] Chrome normal (Local)");
                }
                return new ChromeDriver(options);
            }
        }
    }

    private WebDriver createRemoteDriver(String browser, String gridUrl) {
        try {
            URL endpoint = new URL(gridUrl + "/wd/hub");
            switch (browser.toLowerCase()) {
                case "firefox": {
                    FirefoxOptions options = new FirefoxOptions();
                    RemoteWebDriver driver = new RemoteWebDriver(endpoint, options);
                    System.out.println("[Grid4] Firefox session: " 
                        + driver.getSessionId());
                    return driver;
                }
                default: {
                    ChromeOptions options = new ChromeOptions();
                    // Bắt buộc khi chạy trong Docker container
                    options.addArguments("--no-sandbox");
                    options.addArguments("--disable-dev-shm-usage");
                    RemoteWebDriver driver = new RemoteWebDriver(endpoint, options);
                    System.out.println("[Grid4] Chrome session: " 
                        + driver.getSessionId());
                    return driver;
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Grid URL không hợp lệ: " + gridUrl, e);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE 
                && getDriver() != null) {
            ScreenshotUtil.capture(getDriver(), result.getName());
        }
        if (getDriver() != null) {
            getDriver().quit();
            tlDriver.remove();
        }
    }
}