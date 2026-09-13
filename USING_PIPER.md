Here is the complete process for building Piper with your custom engine and configuring the global command on Ubuntu.

```markdown
**Part 1: Build Piper with Custom espeak-ng on Ubuntu**

**1. Install Ubuntu Build Dependencies**
Update your package list and install the required C++ compilers, CMake, and Python virtual environment tools:

```bash
sudo apt update
sudo apt install build-essential cmake python3-venv python3-pip

```

**2. Update CMakeLists.txt**
Since your folder paths on Ubuntu differ from macOS, update the `CMakeLists.txt` in your `piper1-gpl` repository. Modify the `ExternalProject_Add` block so the copy command points to the absolute Linux path (e.g., `/home/username/espeak-ng`):

```cmake
ExternalProject_Add(espeak_ng_external
    PREFIX ${ESPEAKNG_BUILD_DIR}
    DOWNLOAD_COMMAND ${CMAKE_COMMAND} -E copy_directory "/home/your_username/espeak-ng" <SOURCE_DIR>
    # Keep the original configurations below this point untouched
)

```

**3. Set Up the Python Environment**
Navigate into your `piper1-gpl` repository on the server, create a fresh virtual environment, and activate it:

```bash
python3 -m venv .venv
source .venv/bin/activate

```

**4. Install Build Tools & Compile Piper**
Install the core Python compilation dependencies and run the build command. CMake will copy your custom `espeak-ng` folder and compile the engine natively for Ubuntu:

```bash
pip install scikit-build cmake setuptools wheel ninja
python3 setup.py build_ext --inplace

```

---

**Part 2: Create a Global CLI Command**

**1. Create the Wrapper Script**
Open a new file in your text editor:

```bash
nano piper

```

Paste the following bash script, replacing `PIPER_DIR` with the absolute path to your cloned `piper1-gpl` repository on the Ubuntu server:

```bash
#!/bin/bash
PIPER_DIR="/path/to/your/piper1-gpl"
source "$PIPER_DIR/.venv/bin/activate"
python3 -m piper "$@"

```

Save and exit.

**2. Make it Executable and Global**
Run these commands to grant execution permissions and move the script into your system's binaries folder:

```bash
chmod +x piper
sudo mv piper /usr/local/bin/piper

```

You can now execute `piper` from any directory on your server and stream text files into it natively:

```bash
cat script.txt | piper -m /path/to/model.onnx -f output.wav

```

```

[Improving robot speech with Piper TTS on Ubuntu](https://www.youtube.com/watch?v=q6upRZ0-qpg)
This video provides a practical demonstration of installing and running Piper TTS on an Ubuntu system, including troubleshooting common Linux dependencies.
http://googleusercontent.com/youtube_content/1

```