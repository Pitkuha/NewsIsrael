# NewsIsrael (Java GUI)

## English
NewsIsrael is a Java desktop application with a graphical UI that:
- loads Israel-related news for the current day on startup;
- automatically translates headlines and descriptions into Russian;
- builds a short summary based on fetched news;
- adds a concise end-of-summary conclusion about the current daily picture;
- adds an `AI opinion` block where a model analyzes all daily news and gives its view of the current situation;
- supports a news count filter (5, 10, 15, 20, 30, 50);
- supports day-based tabs (you can open previous dates);
- supports manual refresh via the `Refresh` button.

### Requirements
- JDK 17+
- Maven 3.9+
- Internet access

### Run With Maven
```bash
cd /Users/pitkuha/Desktop/CodexProjects/NewsIsrael
mvn package
java -jar target/news-israel-app-1.0.0.jar
```

### Run In Development Mode
```bash
./run.sh
```

### News Sources
The app uses public RSS/Atom feeds (no API keys required for news fetching).

### AI Analysis
By default, the app tries to generate AI opinion via OpenAI API; if unavailable, it tries a local model via Ollama.

- OpenAI (optional):
  - `OPENAI_API_KEY` — API key
  - `OPENAI_MODEL` — model (default: `gpt-4o-mini`)
  - `OPENAI_BASE_URL` — base URL (default: `https://api.openai.com/v1`)
- Ollama (optional):
  - `OLLAMA_URL` — generation URL (default: `http://localhost:11434/api/generate`)
  - `OLLAMA_MODEL` — model (default: `llama3.1`)

If AI services are unavailable, the app shows a fallback analytical opinion.

### How It Works
- Data is stored in memory only.
- After closing the app, nothing is persisted to disk.
- A separate tab is created for each opened date.

---

## Русский
Приложение на Java с графическим интерфейсом, которое:
- при запуске загружает новости про Израиль за текущие сутки;
- автоматически переводит заголовки и описания новостей на русский язык;
- формирует краткую сводку по найденным новостям;
- добавляет в конце сводки короткий вывод по текущей картине дня;
- добавляет блок `AI-мнение`, где модель анализирует все новости за день и пишет оценку обстановки;
- поддерживает фильтр количества новостей (5, 10, 15, 20, 30, 50);
- поддерживает вкладки по датам (можно открыть предыдущие дни);
- обновляет текущую вкладку кнопкой `Обновить`.

### Требования
- JDK 17+
- Maven 3.9+
- Доступ в интернет

### Запуск через Maven
```bash
cd /Users/pitkuha/Desktop/CodexProjects/NewsIsrael
mvn package
java -jar target/news-israel-app-1.0.0.jar
```

### Запуск в режиме разработки
```bash
./run.sh
```

### Источники новостей
Приложение использует открытые RSS/Atom-ленты (без API ключей для загрузки новостей).

### AI-анализ
По умолчанию приложение пробует получить AI-мнение через OpenAI API, а если сервис недоступен, пытается локальную модель через Ollama.

- OpenAI (опционально):
  - `OPENAI_API_KEY` — ключ API
  - `OPENAI_MODEL` — модель (по умолчанию `gpt-4o-mini`)
  - `OPENAI_BASE_URL` — базовый URL (по умолчанию `https://api.openai.com/v1`)
- Ollama (опционально):
  - `OLLAMA_URL` — URL генерации (по умолчанию `http://localhost:11434/api/generate`)
  - `OLLAMA_MODEL` — модель (по умолчанию `llama3.1`)

Если AI-сервисы недоступны, приложение показывает резервный аналитический вывод.

### Как это работает
- Данные хранятся только в памяти процесса.
- После закрытия приложения ничего не сохраняется на диск.
- Для каждой даты создается отдельная вкладка.
