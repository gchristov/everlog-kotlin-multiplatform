FROM ruby:3.2-slim

WORKDIR /app

# Step 1: Install system dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Step 2: Install Bundler and Fastlane
RUN gem install bundler fastlane

# Step 3: Copy Fastlane files and Gemfile, using project root as build context
COPY fastlane fastlane
COPY Gemfile .

# Step 4: Install plugins
RUN bundle install

# Default command is to execute the firebase deployment lane
CMD ["bundle", "exec", "fastlane", "android", "deploy_firebase", "apk_path:mobile/build/outputs/apk/debug/mobile-debug.apk", "firebase_app_id:1:1234567890:android:dummy_id"]
