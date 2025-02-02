package uk.co.spicule.magnesium_script.expressions;

import org.openqa.selenium.Alert;
import org.openqa.selenium.*;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SendKeys extends Expression {
    enum InputType {
        STRING, SPECIAL
    }

    static Pattern SPECIAL_CHARACTER_PATTERN = Pattern.compile("\\{[a-zA-Z0-9_]+}");

    By locator = null;
    InputType type = InputType.STRING;
    String keys = null;
    Keys specialKeys = null;
    long inputRate = 100; // Delay between keys in ms

    public SendKeys(WebDriver driver, Expression parent) {
        super(driver, parent);
    }

    public SendKeys(WebDriver driver, Expression parent, String keys, @Nullable Long inputRate) {
        super(driver, parent);
        this.keys = keys;
        if(inputRate != null){
            this.inputRate = inputRate;
        }

        parseSpecialKeys();
    }

    public Object execute() {
        LOG.debug("Sending " + type.toString() + ": `" + ((type == InputType.STRING) ? keys : specialKeys) + "` to " + locator + " at a rate of " + inputRate + "ms/char!");

        // Get the web element and send the keys
        WebElement element = driver.findElement(locator);

        // Send the input
        switch (type) {
            case SPECIAL:
                element.sendKeys(specialKeys);
                break;
            case STRING:
                for (Character c : keys.toCharArray()) {
                    element.sendKeys(c.toString());
                    Expression.guardedSleep(inputRate);
                }
                break;
            default:
                throw new RuntimeException("FATAL: Invalid input-type: " + type);
        }

        return null;
    }

    public Object execute(Alert alert) {
        LOG.debug("Resolving expression: `" + this.getClass() + "`!");

        // Send the input
        switch (type) {
            case SPECIAL:
                throw new RuntimeException("Special-characters are not supported for alert-elements!");
            case STRING:
                for (Character c : keys.toCharArray()) {
                    alert.sendKeys(c.toString());
                    Expression.guardedSleep(inputRate);
                }
                break;
            default:
                throw new RuntimeException("FATAL: Invalid input-type: " + type);
        }

        return null;
    }

    public SendKeys parse(Map<String, Object> tokens) throws InvalidExpressionSyntax {
        // Assert the required fields
        assertRequiredField("send-keys", String.class, tokens);
        assertRequiredField("locator-type", String.class, tokens);
        assertRequiredField("locator", String.class, tokens);

        // Populate optional fields
        if(assertOptionalField("input-rate", Integer.class, tokens)) {
            inputRate = Long.parseLong(tokens.get("input-rate").toString());
        }

        // Populate the locator
        locator = Expression.by(tokens.get("locator-type").toString(), tokens.get("locator").toString());

        // Populate the raw keys input
        keys = tokens.get("send-keys").toString();

        // Populate the special keys if it matches the special-keys pattern
        parseSpecialKeys();

        return this;
    }

    private void parseSpecialKeys() {
        Matcher matcher = SPECIAL_CHARACTER_PATTERN.matcher(keys);
        if(matcher.find()) {
            String specialKeyName = keys.substring(1, keys.length() - 1).toUpperCase().replaceAll("-", "_");

            try {
                specialKeys = Keys.valueOf(specialKeyName);
                type = InputType.SPECIAL;
            } catch(IllegalArgumentException e) {
                LOG.warn("send-keys could not identify special character: `" + specialKeyName + "`! Defaulting to interpreting as string literal!");
            }
        }
    }
}
