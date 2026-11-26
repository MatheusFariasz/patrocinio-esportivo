package br.ifsp.edu.scl.patrocinioesportivo.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.Map;

public class BaseSeleniumTest 
{
    protected static final String URL_FRONT = "http://127.0.0.1:5173/";
    protected static final String USUARIO = "teste@teste.com";
    protected static final String DIRETOR = "teste2@teste.com";
    protected static final String SENHA = "Senha123!";
    protected WebDriver driver;

    @BeforeEach
    public void setUp() 
    {
        var opcoes = new ChromeOptions();
        opcoes.setExperimentalOption("prefs", Map.of("profile.password_manager_leak_detection", false));
       
       
        driver = new ChromeDriver(opcoes);
        setInitialPage();
    }
    @AfterEach
    public void tearDown() 
    {
        driver.quit();
    }

    protected void setInitialPage(){}
}
