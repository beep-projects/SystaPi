# Home Assistant Integration for SystaPi

This directory contains the necessary files to integrate your SystaPi with Home Assistant. With the files you can create a dashboard with buttons to change the selected heating program and a schema graphic to display temperatures and other information about your heating system. It is not good looking, but should give you enough information to create your own Home Assistant integration.  
If you have created something nice for your Home Assistant installation, please present it in the [Discussions](https://github.com/beep-projects/SystaPi/discussions) to inspire other users. For questions or feedback regarding this example, please use [this thread](https://github.com/beep-projects/SystaPi/discussions/34).

## Prerequisites

*   A running [Home Assistant](https://www.home-assistant.io/) instance. I use [HAcorePi](https://github.com/beep-projects/HAcorePi) for this, which is a Home Assistant Core installation. You will have to adapt the information given in this readme to match your type of installation.
*   A running [SystaPi](../../README.md) instance that is accessible from your Home Assistant instance.

## Using the example files
This section gives you short instruction on how to use the files, to get more information about the content of each file see [File Descriptions](#file-descriptions). The scripts are create for a Paradigma Systacomfort with menu language set to English. If you have another language set, you need to update the `alias` of the program entries in `systapi_scrips.yaml`

1.  **Get the Systapi Home Assistant helpers:**

    Open a terminal on your Home Assistant machine (or use the SSH addon) and run the following commands:

    ```bash
    curl -L "https://github.com/beep-projects/SystaPi/releases/latest/download/homeassistant.tar.gz"
    tar -xzf  homeassistant.tar.gz
    sudo mkdir -p /home/homeassistant/.homeassistant/www/systapi
    sudo cp ./homeassistant/schema.png /home/homeassistant/.homeassistant/www/systapi
    sudo chown -R homeassistant:homeassistant /home/homeassistant/.homeassistant/www/systapi
    ```

    **Note:** The path `/home/homeassistant/.homeassistant/` might be different depending on your Home Assistant installation method. Please adjust the path accordingly. For example, for Home Assistant OS, the path is `/config/`.

2.  **Include the YAML files:**

    You need to include the provided YAML files in your Home Assistant `configuration.yaml` file. You can do this by adding the following lines to your `configuration.yaml`:

    ```yaml
    # include sensors for reading values from SystaPi vi SystaREST API
    rest: !include systarestapi.yaml
    # includ commands for controlling SystaPi via STouchREST API
    rest_command: !include stouchrestapi.yaml
    # include scripts that trigger STouchREST API commands
    script: !include systapi_scripts.yaml
    ```

    You can either copy the content of `configuration.yaml` from this directory into your main `configuration.yaml` or, if you already have existing `rest`, `rest_command`, or `script` sections, you can merge the contents of the respective files.

    It is recommended to copy all the `.yaml` files from this directory to your Home Assistant configuration directory.
    ```bash
    find ./homeassistant -maxdepth 1 -type f -name "*.yaml" \
    | grep -v "configuration.yaml" \
    | xargs -I{} sudo cp {} /home/homeassistant/.homeassistant/
    sudo chown -R homeassistant:homeassistant /home/homeassistant/.homeassistant/www/systapi
    ```

3.  **Add the dashboard:**

    The `systapi_dashboard.yaml` file contains the definition for a dashboard. You can either use it as a standalone dashboard or copy the parts you want to your existing dashboards.

    To add it as a new dashboard, go to **Settings > Dashboards** in Home Assistant and add a new dashboard with the following properties:

    *   **Title:** SystaPi
    *   **URL:** `systapi`
    *   **Icon:** `mdi:heating-coil`
    *   **File:** `systapi_dashboard.yaml`

## File Descriptions

*   `configuration.yaml`: This file shows how to include the other YAML files in your main Home Assistant `configuration.yaml`.
*   `stouchrestapi.yaml`: This file defines `rest_command` entities that allow Home Assistant to send commands to the SystaPi's STouchREST API. This is used to control the heating system, for example, to change the heating program.
*   `systapi_dashboard.yaml`: This file contains the YAML definition for a Home Assistant dashboard. The dashboard includes buttons to trigger the scripts for changing the heating program and a graphical representation of the heating system's status.
*   `systapi_scripts.yaml`: This file defines several scripts that can be called from Home Assistant. These scripts use the `rest_command` entities from `stouchrestapi.yaml` to execute sequences of commands on the SystaPi to change the heating program.
*   `systarestapi.yaml`: This file defines a set of `rest` sensors that poll the SystaPi's SystaREST API to get the status of the heating system. These sensors provide the data that is displayed on the dashboard.
*   `schema.png`: This is the background image for the heating system schema on the dashboard.
*   `schema.svg`: This is the source file for the schema image. You can use it to create your own customized schema.
