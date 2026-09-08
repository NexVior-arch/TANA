with open('app/src/main/java/com/example/ui/screens/AiScreen.kt', 'r') as f:
    text = f.read()

import re

# The text contains:
# ...
#                     }
#                 }
#             }
#         }
#     }
# }
# 
# @Composable
# private fun CustomAudioWaveformVisualizer(
# ...

split_regex = r'(modifier = Modifier\.size\(16\.dp\)\s*\n\s*\)\s*\n\s*\}\s*\n\s*\}\s*\n\s*\}\s*\n\s*\})'
match = re.search(split_regex, text)
if not match:
    print("Could not find Top Bar end")
    exit(1)

top_part = text[:match.end()]

bottom_part = text[match.end():]

# Find the start of CustomAudioWaveformVisualizer in bottom_part
vis_idx = bottom_part.find('@Composable\nprivate fun CustomAudioWaveformVisualizer')
if vis_idx == -1:
    print("Could not find Visualizer")
    exit(1)

# Just take everything from Visualizer onwards as the real bottom part.
# We also need the closing braces for Column, Box, Scaffold, and AiScreen.
# That is 4 braces.

real_bottom = """
                }
            }
        }
    }
}

""" + bottom_part[vis_idx:]

with open('missing1.txt', 'r') as f:
    m1 = f.read()

with open('missing2.txt', 'r') as f:
    m2 = f.read()

new_text = top_part + "\n" + m1 + "\n" + m2 + "\n" + real_bottom

with open('app/src/main/java/com/example/ui/screens/AiScreen.kt', 'w') as f:
    f.write(new_text)

