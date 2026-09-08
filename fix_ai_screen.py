with open('app/src/main/java/com/example/ui/screens/AiScreen.kt', 'r') as f:
    text = f.read()

import re

# Split at the end of Top Bar
split_regex = r'(modifier = Modifier\.size\(16\.dp\)\s*\n\s*\)\s*\n\s*\}\s*\n\s*\}\s*\n\s*\}\s*\n\s*\}\s*\n\s*\})'
match = re.search(split_regex, text)
if not match:
    print("Could not find Top Bar end")
    exit(1)

top_part = text[:match.end()]

bottom_part = text[match.end():]
# bottom_part has some closing braces and then ActionPlanCard (wait! ActionPlanCard is in the bottom part?)
# Yes! Because my sed command only deleted up to CustomAudioWaveformVisualizer!
# WAIT! Let me check if ActionPlanCard is still in the file.
