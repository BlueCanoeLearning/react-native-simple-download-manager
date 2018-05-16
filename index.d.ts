declare module "react-native-simple-download-manager" {

    export interface ResponseStatus {
        status: "STATUS_PENDING" | "STATUS_RUNNING" | "STATUS_SUCCESSFUL" | "STATUS_UNKNOWN" | "STATUS_PAUSED" | "STATUS_FAILED";
        reason: string;
        downloadId: string;
    }

    export interface RequestConfig {
        downloadTitle: string;
        downloadDescription: string;
        saveAsName:  string;
        allowedInRoaming: boolean;
        allowedInMetered: boolean;
        showInDownloads: boolean;
        external: boolean; //when false basically means use the default Download path (version ^1.3)
        path: string; //if "external" is true then use this path (version ^1.3)
    }

    export const queueDownload: (url: string, headers?: Object, config?: Partial<RequestConfig>) => Promise<ResponseStatus>;
    export const download: (url: string, headers?: Object, config?: Partial<RequestConfig>) => Promise<ResponseStatus>;
    export const attachOnCompleteListener: (downloadId: string) => Promise<ResponseStatus>;
    export const cancel: (downloadId: string) => Promise<ResponseStatus>;
    export const checkStatus: (downloadId: string) => Promise<ResponseStatus>;
}

