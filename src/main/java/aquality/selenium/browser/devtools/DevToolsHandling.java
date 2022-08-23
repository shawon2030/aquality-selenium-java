package aquality.selenium.browser.devtools;

import aquality.selenium.browser.AqualityServices;
import aquality.selenium.core.localization.ILocalizedLogger;
import org.apache.commons.lang3.NotImplementedException;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.Command;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.Event;
import org.openqa.selenium.devtools.HasDevTools;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Wrapper for Selenium {@link DevTools} functionality.
 */
public class DevToolsHandling {
    private final HasDevTools devToolsProvider;
    private final ILocalizedLogger logger;

    private DevTools session;
    private NetworkHandling network;
    private EmulationHandling emulation;

    /**
     * Initializes an instance of {@link DevToolsHandling}
     * @param devToolsProvider Instance of {@link org.openqa.selenium.WebDriver} which supports CDP protocol.
     */
    public DevToolsHandling(HasDevTools devToolsProvider) {
        this.devToolsProvider = devToolsProvider;
        this.logger = AqualityServices.getLocalizedLogger();
    }

    private DevTools getDevTools() {
        if (session == null) {
            session = devToolsProvider.getDevTools();
        }
        return session;
    }

    private void logCommand(String commandName, Map<String, Object> commandParameters) {
        if (!commandParameters.isEmpty()) {
            logger.info("loc.browser.devtools.command.execute.withparams", commandName, commandParameters);
        }
        else {
            logger.info("loc.browser.devtools.command.execute", commandName);
        }
    }

    private void logCommandResult(Object result) {
        if (result != null) {
            if (result instanceof Map && ((Map<?, ?>) result).isEmpty()) {
                return;
            }
            logger.info("loc.browser.devtools.command.execute.result", result);
        }
    }

    /**
     * Creates a session to communicate with a browser using the Chromium Developer Tools debugging protocol, if there is not one.
     * If the session already exist, returns existing session.
     * Defaults to autodetect the protocol version for Chromium, V85 for Firefox.
     * @return The active session to use to communicate with the Chromium Developer Tools debugging protocol.
     */
    public DevTools getDevToolsSession() {
        logger.info("loc.browser.devtools.session.get", "default");
        getDevTools().createSessionIfThereIsNotOne();
        return session;
    }

    /**
     * Creates a session to communicate with a browser using the Chromium Developer Tools debugging protocol, if there is not one, on given window/tab (aka target).
     * If the session already exist, returns existing session.
     * If windowHandle is null, then the first "page" type will be selected.
     * Pass the windowHandle if you have multiple windows/tabs opened to connect to the expected window/tab.
     * Defaults to autodetect the protocol version for Chromium, V85 for Firefox.
     * @param windowHandle result of {@link org.openqa.selenium.WebDriver#getWindowHandle()}, optional.     *
     * @return The active session to use to communicate with the Chromium Developer Tools debugging protocol.
     */
    public DevTools getDevToolsSession(String windowHandle) {
        logger.info("loc.browser.devtools.session.get", windowHandle);
        getDevTools().createSessionIfThereIsNotOne(windowHandle);
        return session;
    }

    /**
     * Closes a DevTools session.
     */
    public void closeDevToolsSession() {
        logger.info("loc.browser.devtools.session.close");
        getDevTools().disconnectSession();
    }

    /**
     * Executes a custom Chromium Dev Tools Protocol Command.
     * Note: works only if current driver is instance of {@link ChromiumDriver}.
     * @param commandName Name of the command to execute.
     * @param commandParameters Parameters of the command to execute.
     * @return An object representing the result of the command, if applicable.
     */
    public Map<String, Object> executeCdpCommand(String commandName, Map<String, Object> commandParameters) {
        if (devToolsProvider instanceof ChromiumDriver) {
            logCommand(commandName, commandParameters);
            ChromiumDriver driver = (ChromiumDriver) devToolsProvider;
            Map<String, Object> result = driver.executeCdpCommand(commandName, commandParameters);
            logCommandResult(result);
            return result;
        }
        else {
            throw new NotImplementedException("Execution of CDP command directly is not supported for current browser. Try sendCommand method instead.");
        }
    }

    /**
     * Sends the specified command and returns the associated command response.
     * @param command An instance of the {@link Command} to send.
     * @param <X> The type of the command's result. For most commands it's {@link Void}
     * @return the result of the command, if applicable
     */
    public <X> X sendCommand(Command<X> command) {
        logCommand(command.getMethod(), command.getParams());
        X result = getDevToolsSession().send(command);
        logCommandResult(result);
        return result;
    }

    /**
     * Adds a listener for a specific event with the handler for it.
     * @param event Event to listen for
     * @param handler Handler to call
     * @param <X> The type of the event's result.
     */
    public <X> void addListener(Event<X> event, Consumer<X> handler) {
        logger.info("loc.browser.devtools.listener.add", event.getMethod());
        getDevToolsSession().addListener(event, handler);
    }

    /**
     * Removes all the listeners, and disables all the domains.
     */
    public void clearListeners() {
        logger.info("loc.browser.devtools.listener.clear");
        getDevToolsSession().clearListeners();
    }

    /**
     * Version-independent emulation DevTools commands.
     * @return an instance of {@link EmulationHandling}.
     */
    public EmulationHandling emulation() {
        if (emulation == null) {
            emulation = new EmulationHandling(this);
        }
        return emulation;
    }

    /**
     * DevTools commands for network requests interception.
     * @return an instance of {@link NetworkHandling}.
     */
    public NetworkHandling network() {
        if (network == null) {
            network = new NetworkHandling(this);
        }
        return network;
    }
}
