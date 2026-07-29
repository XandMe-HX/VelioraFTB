package id.velioragardens.velioraftb.util;

import id.velioragardens.velioraftb.VelioraFTB;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class TextUtil {
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();
    private static final DecimalFormat MONEY;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        symbols.setGroupingSeparator('.');
        MONEY = new DecimalFormat("#,##0", symbols);
    }

    private TextUtil() {
    }

    public static Component color(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    public static void send(VelioraFTB plugin, CommandSender sender, String path) {
        send(plugin, sender, path, Map.of());
    }

    public static void send(
            VelioraFTB plugin,
            CommandSender sender,
            String path,
            Map<String, String> placeholders
    ) {
        String raw = plugin.getConfig().getString(path, "");
        if (raw == null || raw.isEmpty()) {
            return;
        }

        Map<String, String> values = new HashMap<>(placeholders);
        values.putIfAbsent("prefix", plugin.getConfig().getString("messages.prefix", "&6[VelioraFTB] &r"));
        sender.sendMessage(color(replace(raw, values)));
    }

    public static String replace(String text, Map<String, String> placeholders) {
        String result = text == null ? "" : text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public static String formatMoney(double amount) {
        synchronized (MONEY) {
            return MONEY.format(amount);
        }
    }

    public static String formatDuration(long milliseconds) {
        if (milliseconds <= 0) {
            return "0 menit";
        }

        long totalMinutes = Math.max(1L, (milliseconds + 59_999L) / 60_000L);
        long days = totalMinutes / 1_440L;
        long hours = (totalMinutes % 1_440L) / 60L;
        long minutes = totalMinutes % 60L;

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append(" hari ");
        }
        if (hours > 0) {
            result.append(hours).append(" jam ");
        }
        if (minutes > 0 || result.isEmpty()) {
            result.append(minutes).append(" menit");
        }
        return result.toString().trim();
    }
}
