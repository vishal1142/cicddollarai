#!/bin/bash

# -----------------------------------------
# Auto Git Commit & Push Script
# For: https://github.com/vishal1142/java.git
# -----------------------------------------

# Set repository URL
REPO_URL="https://github.com/vishal1142/java.git"

# Default commit message with timestamp
DEFAULT_COMMIT_MSG="Auto-commit to java repo on $(date '+%Y-%m-%d %H:%M:%S')"

# Ask user for a custom commit message
read -p "Enter commit message (or press Enter for default): " COMMIT_MSG

# Use default if input is empty
if [ -z "$COMMIT_MSG" ]; then
  COMMIT_MSG="$DEFAULT_COMMIT_MSG"
fi

# Initialize Git repo if not already initialized
if [ ! -d ".git" ]; then
  echo "📁 Initializing Git repository..."
  git init
  git branch -M main
  git remote add origin "$REPO_URL"
else
  echo "✅ Git repository already initialized."
fi

# Add and commit changes
git add .
git commit -m "$COMMIT_MSG"

# Set remote URL in case it changed
git remote set-url origin "$REPO_URL"

# Try to detect remote HEAD branch, fallback to 'main'
DEFAULT_BRANCH=$(git remote show origin 2>/dev/null | grep 'HEAD branch' | awk '{print $NF}')
if [ -z "$DEFAULT_BRANCH" ]; then
  DEFAULT_BRANCH="main"
fi

# Push changes and set upstream if needed
echo "📤 Pushing to branch: $DEFAULT_BRANCH"
git push -u origin "$DEFAULT_BRANCH"

# Output summary
echo "✅ Code pushed to GitHub!"
echo "Repository: $REPO_URL"
echo "Branch: $DEFAULT_BRANCH"
echo "Commit message: $COMMIT_MSG"
echo "Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"
echo "----------------------------------------"
echo "Push completed successfully!"
echo "----------------------------------------"
echo "Visit your GitHub repo to see the changes."
echo "----------------------------------------"
echo "Happy coding! 🚀"
echo "----------------------------------------"
echo "Note: Ensure you have the necessary permissions to push to this repository."
echo "----------------------------------------"
echo "If you encounter any issues, please check your Git configuration and remote settings."
