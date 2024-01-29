package eu.pb4.banhammer.impl.discord;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public final class DiscordWebhookMessage {
    private static final Gson GSON = new Gson();

    public static DiscordWebhookMessage of() {
        return new DiscordWebhookMessage();
    }

    public String content;
    public String username;
    @SerializedName("avatar_url")
    public String avatarUrl;
    public boolean tts = false;
    public List<Embed> embeds;
    @SerializedName("allowed_mentions")
    public AllowedMentions allowedMentions = new AllowedMentions();

    public DiscordWebhookMessage content(String content) {
        this.content = content;
        return this;
    }

    public DiscordWebhookMessage username(String content) {
        this.username = content;
        return this;
    }

    public DiscordWebhookMessage avatar(String content) {
        this.avatarUrl = content;
        return this;
    }

    public DiscordWebhookMessage embed(Embed embed) {
        if (this.embeds == null) {
            this.embeds = new ArrayList<>();
        }
        this.embeds.add(embed);

        return this;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static class AllowedMentions {
        public String[] parse = new String[]{};
    }

    public static class Embed {
        public String title;
        public String description;
        public String url;
        public String timestamp;
        public int color;
        public Footer footer;
        public Image image;
        public Image thumbnail;
        public Author author;
        public List<Field> fields;

        public Embed title(String content) {
            this.title = content;
            return this;
        }

        public Embed description(String content) {
            this.description = content;
            return this;
        }

        public Embed url(String content) {
            this.url = content;
            return this;
        }

        public Embed timestamp(String content) {
            this.timestamp = content;
            return this;
        }

        public Embed color(int content) {
            this.color = content;
            return this;
        }

        public Embed footer(String text) {
            this.footer = new Footer();
            this.footer.text = text;
            return this;
        }

        public Embed footer(String text, String url) {
            this.footer(text);
            this.footer.iconUrl = url;
            return this;
        }

        public Embed image(String content) {
            this.image = new Image();
            this.image.url = content;
            return this;
        }

        public Embed thumbnail(String content) {
            this.thumbnail = new Image();
            this.thumbnail.url = content;
            return this;
        }

        public Embed author(String content, String icon, String url) {
            this.author = new Author();
            this.author.name = content;
            this.author.iconUrl = icon;
            this.author.url = url;
            return this;
        }

        public Embed field(String key, String value, boolean inline) {
            if (this.fields == null) {
                this.fields = new ArrayList<>();
            }
            var field = new Field();
            field.name = key;
            field.value = value;
            field.inline = inline;
            this.fields.add(field);

            return this;
        }

        public static class Footer {
            public String text = "";
            @SerializedName("icon_url")
            public String iconUrl;
        }

        public static class Image {
            public String url = "";
            public int height;
            public int width;
        }

        public static class Author {
            public String name = "";
            public String url;
            @SerializedName("icon_url")
            public String iconUrl;
        }

        public static class Field {
            public String name = "";
            public String value = "";
            public boolean inline;
        }
    }
}