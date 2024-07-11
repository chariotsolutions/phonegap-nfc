<template>
  <div @click="props.isWriteOnCard ? writeProductOnCard() : scanNfc()">
        {{ props.isWriteOnCard ? "Write on Card" : "Scan NFC" }}
  </div>

  <ion-modal ref="modal" :onDidDismiss="close" :can-dismiss="true" :is-open="isAndroidScanOpen"
    class="modalResponsive opacityHigh" :initial-breakpoint="1" :breakpoints="[0, 1]">
    <div class="ion-padding">
      <div
        class="flex items-center min-w-[120px] mt-4 font-bold text-Helvetica text-[28px] rounded-[6px] p-[10px] text-c_f_p w-[100%] md:w-auto justify-center transition ease-in-out delay-150">
        Ready to Scan
      </div>
      
      <div
        class="flex items-center min-w-[120px] mt-4 font-bold text-[16px] text-Helvetica rounded-[6px] p-[10px] text-c_f_s w-[100%] md:w-auto justify-center transition ease-in-out delay-150">
        Hold near writable NFC tag to activate the product.
      </div>
      <button @click="close"
        class="flex items-center min-w-[120px] mt-4 font-bold bg-c_p text-Helvetica text-sm rounded-[6px] p-[10px] text-[#fff] w-[100%] md:w-auto justify-center transition ease-in-out delay-150 hover:bg-c_h_p">
        {{ trans("Cancel") }}
      </button>
    </div>
  </ion-modal>
</template>

<script setup>
import { IonModal } from "@ionic/vue";
import { onMounted, ref } from "vue";

import { Capacitor } from "@capacitor/core";
import { Ndef, NFC } from "@ionic-native/nfc";
import { Toast } from "@capacitor/toast";
import { trans } from "@/utils/translation";

const emit = defineEmits(["activated"]);

const isAndroidScanOpen = ref(false);
const props = defineProps({
    isWriteOnCard: Boolean,
});
var writer_listner;
const onNdefEvent = (nfcEvent) => {
  console.log("nfcEvent", nfcEvent);
  if (!isAndroidScanOpen.value) {
    isAndroidScanOpen.value = true;
  }
};

const close = () => {
  writer_listner?.unsubscribe();
  isAndroidScanOpen.value = false;
};

const scanNfc = () => {
  if (Capacitor.getPlatform() == "ios") {
    NFC.enabled()
      .then(() => {
        NFC.scanTag()
          .then((data) => {
            const message = NFC.bytesToString(data.ndefMessage[0].payload);
            onScan(message);
          })
          .catch((err) => {
            Toast.show({
              text: "Error scanning NFC tag",
              duration: "short",
            });
            close();
          });
      })
      .catch((err) => {
        Toast.show({
          text: "NFC not available",
          duration: "short",
        });
        close();
      });
  } else {
    writer_listner = null;
    writer_listner = NFC.addNdefListener(
      onNdefEvent,
      () => {
        // console.log("success");
      },
      (err) => {
        // console.log("error", err);
      }
    ).subscribe(
      (data) => {
        // console.log("nfc_success", data);
        const message = data.tag.ndefMessage;
        // const id = NFC.bytesToString(data.tag.id);
        const messageText = NFC.bytesToString(message[0].payload);
        // console.log("nfc_id", id);
        onScan(messageText);
        close()
      },
      (err) => {
        // console.log("nfc_error", err);
        close();
      }
    );
  }
};

const onScan = (result) => {
  if (result) {
      // Perform your desired action with the result
    console.log("result", result);
  }else{
    Toast.show({
      text: "Error scanning NFC tag",
      duration: "short",
    });
  
  }
};



const writeProductOnCard = () => {
    const nfc_url = "https://www.google.com";
  return new Promise((resolve, reject) => {
    if (Capacitor.getPlatform() != "ios") {
      write(nfc_url).then((data) => {
        console.log("writeProductOnCard success", data);
        resolve(data);
      }).catch((err) => {
        close();
        console.log("writeProductOnCard error", err);

        reject(err);
      });
      return;
    }
    const message = [Ndef.uriRecord(nfc_url)];
    setTimeout(() => {
      NFC.write(message).then((data) => {
        // alert(JSON.stringify(data));
        resolve(data);
      }).catch((err) => {
        Toast.show({
          text: "Canceled writing NFC tag",
          duration: "short",
        });

        reject(err);
      });
    }, 3000);
  });
};

var writer_listner;
const write = async (nfc_url) => {
  return new Promise((resolve, reject) => {
    writer_listner = null;
    // check if nfc is available on android native device
    NFC.enabled()
      .then((data) => {
        console.log("nfc_enabled", data);
        if (data) {
          writer_listner = NFC.addNdefListener(
            onNdefEvent,
            () => {
              console.log("success");
            },
            (err) => {
              console.log("error", err);
            }
          ).subscribe(
            (data) => {
              console.log("writing", data);
              NFC.write([Ndef.uriRecord(nfc_url)])
                .then((data) => {
                  Toast.show({
                    text: "NFC tag written successfully",
                    duration: "short",
                  });
                  console.log("nfc_success", data);
                  resolve(data);
                  // stop listening
                  close();
                })
                .catch((err) => {
                  Toast.show({
                    text: "NFC tag write error",
                    duration: "short",
                  });
                  console.log("nfc_error", err);
                  close();
                  reject(err);
                  
                });
            },
            (err) => {
                close();
              reject(err);
              console.log("nfc_error", err);
            }
          );
        }
      })
      .catch((err) => {
        Toast.show({
          text: "NFC not available",
          duration: "short",
        });
        close();
        reject(err);
      });
  });
};


onMounted(() => {
  window.scrollTo({ top: 0, behavior: "smooth" });
});
</script>
