FROM python:3.12-slim

WORKDIR /app

# Copy requirements first to leverage Docker cache
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy all scripts from the current directory (the build context) into /app
COPY . .

ENTRYPOINT ["python", "export_import_manage.py"]
