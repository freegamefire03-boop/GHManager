⚠️ CRITICAL ARCHITECTURE RULES — READ BEFORE CODING:

"Clone to Phone" Implementation: Do NOT use heavy native git libraries like JGit. Instead, implement the cloning feature by downloading the repository archive as a ZIP file via the GitHub API and unpacking it directly to the local Android storage.

Strictly No UI Polish (Functionality First): Keep the user interface completely barebones and structural. Avoid custom animations, custom shapes, gradients, or heavy design elements for now. Focus 100% on the MutableState logic, tab layouts, reactive token switching, and stable state management. We will polish the colors and styles only after the core system is fully functional.