# System Context (C4 Level 1)

The Local File Search Engine is a standalone desktop application that:

- Is used by a **Local User** via the command line
- Interacts with the **Local File System** to crawl files
- Stores data into a local **Postgres Database**

## Actors & Systems:

- **Local User** → uses CLI to search files
- **File System** → supplies the `.txt` files
- **Postgres DB** → stores indexed content & metadata

![Alt Text](/Users/lorenabuzea/Desktop/UTCN/AN3/SEM2/SD/Project Files/SystemContext.png)
---

#  Container View (C4 Level 2)

The main application consists of the following containers:

- **File Crawler** – walks through directories and gathers text files
- **Indexer** – extracts metadata and stores it in DB
- **Search Engine** – processes queries and retrieves results
- **CLI Interface** – handles user input and result display
- **Database (SQLite)** – stores indexed data with support for search queries

![Alt Text](/Users/lorenabuzea/Desktop/UTCN/AN3/SEM2/SD/Project Files/Containers.png)

---

#  Component View (C4 Level 3)

## File Crawler

- **DirectoryWalker** – Recursively scans directories
- **FileValidator** – Validates file types/extensions

## Indexer

- **MetadataExtractor** – Extracts file metadata
- **ContentReader** – Reads text content
- **DBWriter** – Inserts data into the SQLite database

## Search Engine

- **QueryProcessor** – Parses user queries
- **ResultRetriever** – Runs SQL/full-text searches
- **SnippetGenerator** – Returns 3-line content previews

## GUI Interface

- **UserInput** – Reads input from the terminal
- **ResultDisplay** – Prints formatted results

## Database

- **DatabaseManager** – Handles connection and query execution

![Alt Text](/Users/lorenabuzea/Desktop/UTCN/AN3/SEM2/SD/Project Files/Components.png)

---

#  Class View (C4 Level 4)

Each component is mapped to classes and method responsibilities:

## **File Crawler**

### `DirectoryWalker`
- This class is responsible for **navigating the file system**.
- It takes a root directory as input and goes through all subdirectories.
- It returns a list of file paths that match a certain pattern (e.g., `.txt` files).

### `FileValidator`
- Works hand-in-hand with `DirectoryWalker`.
- Filters the list of files by checking if they meet the criteria (e.g., file extension, non-hidden, not binary).
- Prevents unwanted files from being processed further.

---

##  **Indexer**

### `MetadataExtractor`
- Pulls **file-related data** like:
    - File name
    - Path
    - Size
    - Last modified timestamp
    - Extension
- This metadata is useful for filtering and future features like sorting or tagging.

### `ContentReader`
- Reads the **text content** of each valid file.
- Deals with encoding (like UTF-8) and skips files that can't be read as text.
- Returns the file's raw text as a string.

### `DBWriter`
- Takes the file’s content and metadata and **stores it into the database**.
- Responsible for formatting the data as needed and handling database insertion.
- Ensures that no duplicate records are inserted.

---

## **Search Engine**

### `QueryProcessor`
- Prepares the search query from the user input.
- May clean up the string, tokenize it, or convert it into a SQL-compatible format.
- Makes the search more effective by normalizing input.

### `ResultRetriever`
- Executes the search on the database using the processed query.
- Returns a list of files that contain the keyword or phrase.
- Can be improved later to support ranking or advanced search logic.

### `SnippetGenerator`
- Takes the file content and the user’s query and finds a **short preview**.
- Typically returns the first 3 lines of the file or lines surrounding the match.
- Improves user experience by giving context before opening the file.

---

##  **User Interface (GUI)**

### `UserInput`
- Manages the interface where the user types in their query.
- Can be as simple as `Scanner` in CLI or a text field in a GUI.
- Passes the input to the query processor.

### `ResultDisplay`
- Takes the search results and formats them for the user.
- Displays file path and snippet.
- Can be designed to highlight keywords or show previews in a cleaner format.

---

##  **DatabaseManager**

- This class sets up and manages the **SQLite database connection**.
- Handles basic operations like:
    - Connecting to the DB
    - Inserting records
    - Running SELECT queries
- Acts as the gateway between the application and the database.

---

##  **SearchResult (Support Class)**

- Represents each item found in a search.
- Contains at least:
    - The full path to the file
    - A short snippet of its content
- Makes it easier to pass around result data between classes like `ResultRetriever` and `ResultDisplay`.
