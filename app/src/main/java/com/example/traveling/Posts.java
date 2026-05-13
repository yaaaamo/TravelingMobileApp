package com.example.traveling;

public class Posts {
        private String username;
        private String caption;
        private int imageRes;

        public Posts(String username, String caption, int imageRes) {
            this.username = username;
            this.caption = caption;
            this.imageRes = imageRes;
        }

        public String getUsername() {
            return username;
        }

        public String getCaption() {
            return caption;
        }

        public int getImageRes() {
            return imageRes;
        }
}
